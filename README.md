Jubilee
=========

A fast rack server build upon [vertx](http://vertx.io)

Issues
----------

* Long running request get reset, Thread pool execute time
* Failed to serve uploaded images
* Rack handler still need a latch

Installation
-----------

```gem install jubilee```

TODO
----------

* EventBus
* WebSocket [need test]

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
