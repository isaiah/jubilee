Jubilee
=========

A fast rack server build upon [vertx](http://vertx.io)

Issues
----------

* Direct use of HttpServerResponse object in Ruby Response class is
  potentially slow. see [Improving Java Integration
  Performance](https://github.com/jruby/jruby/wiki/ImprovingJavaIntegrationPerformance)

TODO
----------

* Daemon mode
* Try non-block IO
* site
* benchmark: Get, static file, post

* EventBus
* WebSocket [need test]

Fixed
-----------

* Long running request get reset, connection timeout. Fix by increase
  connection timeout from 5 seconds to 10 seconds, cannot be higher, or
  it just doesn't respond on first request.  donno why.
* Failed to serve uploaded images. Fixed by use vertx sendfile
* Rack handler still need a latch. Fixed by execute a hook in
  server#stop

Installation
-----------

```gem install jubilee```

Performance
===========

Get request for test/sinatra_app
-----------

Got rival performance as puma.
(ab -c 20 -n 10000)

jubilee: 1493rps after warm
puma: 1327rps after warm

unicorn (worker 10): 1440rps

Serve static file
-----------

Requirement
===========

JRuby '~> 1.7.0'
