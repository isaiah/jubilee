Jubilee
=========

A rack server build upon [vertx](http://vertx.io)


Features
-----------

* Async io
* EventBus [TBD]

Performance
-----------

Got rival performance as puma.
(ab -c 20 -n 10000)

jubilee: 1146rps after warm
puma: 1327rps after warm

unicorn (worker 10): 1440rps

