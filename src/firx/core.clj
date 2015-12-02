(ns firx.core
  "A small piece of code to show what an Application Gateway is
  after a question raise in the \"Barcelona Software Craftmanship\" 
  Meetup on 2015-Nov-30. 
  
  The example consists of two ring-based servers. The first one
  `webapp` just waits for HTTP requests in the form 
  `/uppercase/word` to return a JSON map with the word in  
  uppercase form. 
  The second one, `appgw`, is the application gateway (l7 firwall,
  WAF, etc). It checks if the word is allowed (so not beloging
  to a list of problematic words that the server loads at startup 
  time) and, if so, forwards the request to `webapp`. To show
  the multiple potential uses of an Application Gateway, 
  it also caches the result.
  "
  (:require [org.httpkit.client :as http]
            [taoensso.timbre :refer [debug]]
            [clojure.data.json :as json]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.util.response :as r]
            [clojure.string :as s]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [environ.core :refer [env]]))


;; Back-end server with a very simple API
;; /uppercase/:word will return an upper-cased version
;; of :word

(defroutes webapp
  (GET "/uppercase/:text" [text] 
       (do
         (debug "Received request for" text)
         (-> (r/response (json/write-str {:status :ok :text (s/upper-case text)}))
             (r/content-type "application/json"))))
  (route/not-found
    (do 
      (debug "Unknown route")
      (-> (r/response (json/write-str {:status :err :reason :op-not-found}))
          (r/content-type "application/json")))))


;; Cache and forbidden words set
(def words (atom #{}))
(def cache (atom {}))

(defn load-forbidden-words
  []
  (debug "loading config file:" (env :gw-cfg))
  (when (env :gw-cfg) 
    (-> (env :gw-cfg) 
        io/resource
        slurp
        edn/read-string
        :forbidden-words)))

(defn init-appgw
  []
  (let [words-cfg (load-forbidden-words)]
    (when words-cfg
      (debug words-cfg)
      (swap! words into words-cfg))))


(defn forbidden?
  [w]
  (get @words w))

(defn forbidden-response
  [w]
  (debug "Attempt to request a forbidden word" w)
  (let [js (json/write-str {:status :err :reason "forbidden word"})]
    (-> (r/response js)
        (r/content-type "application/json")
        (r/status 403))))

(defn cached?
  [w]
  (get @cache w))


(defn cached-response
  [w]
  (debug "Returning cached word" w)
  (let [js (json/write-str {:status :ok :cached true :text (get @cache w)})]
    (-> (r/response js)
        (r/content-type "application/json")
        (r/status 200))))

(defn response->word
  [resp]
  (-> resp
      :body
      (json/read-str :key-fn keyword)
      :text))

(defn non-cached-response
  [w]
  (debug "Returning non-cached word" w)
  (let [js (json/write-str {:status :ok :cached false :text w})]
    (-> (r/response js)
        (r/content-type "application/json")
        (r/status 200))))

(defn error-response
  [w reason]
  (debug "Returning error for " w " because of" reason)
  (let [js (json/write-str {:status :err :reason reason})]
    (-> (r/response js)
        (r/content-type "application/json")
        (r/status 500))))

(defn update-cache!
  [w u]
  (swap! cache assoc w u))
   
(defn forward-request-and-cache!
  "Forwards the request to the application server and 
  updates de cache."
  [w port]
  (let [url (format "http://localhost:%s/uppercase/%s" (str port) w)
        _ (debug "Forwarding request for " w " to" url)
        http-resp  @(http/get url)]
    (debug "Received response. Status: " (:status http-resp))
    (if (= 200 (:status http-resp))
      (if-let [resp-word (response->word http-resp)]
        (do 
          (update-cache! w resp-word)
          (non-cached-response resp-word))
        (error-response w "badly formatted response from downstream server"))
      (error-response w "downstream server did not return a 200 response"))))

(defn process-request
  "The heart of the Application Gateway. Checks if word is not _forbidden_
  and, if it "
  [word]
  (debug "Received request for word" word)
  (cond
    (forbidden? word) (forbidden-response word)
    (cached? word) (cached-response word)
    :else (forward-request-and-cache! word (env :forward-port))))

;; The Application Gateway
(defroutes appgw
  (GET "/uppercase/:text" [text] 
       (process-request text))

  (route/not-found 
    (-> (r/response "NOT FOUND"))))
