Jubilee
=========

A fast rack server build upon [vertx](http://vertx.io)

TODO
----------

* Daemon mode (WIP)
* [TeeInput](https://github.com/defunkt/unicorn/blob/master/lib/unicorn/tee_input.rb)
* Site(WIP)
* benchmark: Get, static file, post

* EventBus
* WebSocket 

Fixed
-----------

* Long running request get reset, as connection timed out. Fix by increase
  default connection timeout from 5 seconds to 10 seconds, cannot be higher, or
  it just doesn't respond.  donno why.
* Failed to serve uploaded images. Fixed by use vertx sendfile
* Rack handler still need a latch. Fixed by execute a unblock hook in
  server#stop
* If-Modified-Since doesn't work. All headers were added.


Installation
-----------

```gem install jubilee```

Performance
===========

Get request for test/sinatra_app
-----------

Got rival performance as puma.
(ab -c 20 -n 10000)

jubilee: 1750rps after warm
puma: 1327rps after warm

unicorn (worker 10): 1440rps

Requirement
===========

JRuby '~> 1.7.0'

License
========

The same as JRuby and vertx


Kudos
========

Inspired by [this
post](http://blog.jayfields.com/2012/05/how-i-open-source.html), I
decide to release it early
