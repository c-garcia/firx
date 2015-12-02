# firx

A small piece of code to show what an Application Gateway is
after a question raised in the "Barcelona Software Craftmanship" 
Meetup on 2015-Nov-30. 

The example consists of two ring-based servers. The first one
`webapp` just waits for HTTP requests in the form 
`/uppercase/word` to return a JSON map with the word in uppercase form. 
The application, however, does nothing to block any request or to
optimize performance by any means possible. Any well-formed request
will be answered.

The second one, `appgw`, is the Application Gateway (l7 firwall,
WAF, etc). It is there to exemplify some form of filterint and optimization 
as these tools tend to provide. It checks if the word is an allowed one (so 
not beloging to a list of problematic words that the server loads at startup 
time) and, if so, forwards the request to `webapp`. To show 
the multiple potential uses of an Application Gateway, 
it also caches the result.

The Application Gateway, therefore, is able to perform many functions 
a tightly integrated way with the business logic provided by other 
elements sitting behind it.

## Usage

Open multiple sessions to start the different servers. Logging is sent to the console.

* Start the app server `lein with-profile webapp ring server-headless 3000`
* Start the app gw `FORWARD_PORT=3000 lein with-profile +appgw ring server-headless 4000`
* Raise a request on a allowed word `curl -q -L -o - http://localhost:4000/uppercase/guitar`
   * The result is not cached `{"status":"ok","cached":false,"text":"GUITAR"}`
* Raise it again
   * The result is cached `{"status":"ok","cached":true,"text":"GUITAR"}`
* Raise a request on a _non-allowed_ word `curl -q -L -o - http://localhost:4000/uppercase/disco`
   * Error is returned `{"status":"err","reason":"forbidden word"}`

## The name

Firx is a small, semi-hostile alien attached to the liver of Cugel the Clever, the main character in the _Eyes of Overworld_, a
novel written by Jack Vance. My favourite sci-fi author. 

## License

Copyright © 2015 Cristobal Garcia

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
