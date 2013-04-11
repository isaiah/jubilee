Jubilee
=========

A fast rack server build upon [vertx](http://vertx.io)

Installation
-----------

```gem install jubilee```

Performance
-----------

Get request for [sinatra test app](https://github.com/isaiah/jubilee/tree/master/test/sinatra_app):

with ```ab -c 20 -n 10000```

1750rps after warm

unicorn (worker 10): 1440rps

Requirement
-----------

java7 or above
JRuby '~> 1.7.0'

License
--------

The same as JRuby and vertx
