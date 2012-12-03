Jubilee
=========

A rack server build upon [vertx](http://vertx.io)


Features
-----------

* SSL
* Chunked body
* WebSocket
* Async io
* EventBus [TBD]

Performance
-----------

Got rival performance as puma.
(ab -c 20 -n 10000)

jubilee: 1193rps after warm
puma: 1327rps after warm

unicorn (worker 10): 1440rps

