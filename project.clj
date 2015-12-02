(defproject firx "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [http-kit "2.1.18"]
                 [com.taoensso/timbre "4.1.4"]
                 [org.clojure/data.json "0.2.6"]
                 [ring "1.4.0"]
                 [compojure "1.4.0"]
                 [org.clojure/tools.nrepl "0.2.11"]
                 [environ "1.0.1"]]
  :plugins [[lein-environ "1.0.1"]]
  :profiles {:dev {:plugins [[lein-ring "0.9.7"]]}
             :appgw {:ring {:handler firx.core/appgw
                             :init firx.core/init-appgw}
                      :env {:gw-cfg "appfw.edn"}}
             :webapp {:ring {:handler firx.core/webapp}}})
