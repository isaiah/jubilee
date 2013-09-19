[![Build Status](https://travis-ci.org/isaiah/jubilee.png?branch=master)](https://travis-ci.org/isaiah/jubilee)

Jubilee
=========

An experimental rack server built upon [vertx 2.0](http://vertx.io).

Why another rack server?
------------------------

"Vert.x is a lightweight, high performance application platform for the JVM
that's designed for modern mobile, web, and enterprise applications."
      -- vertx.io site

In short, Vertx is nodejs on the JVM, only much more faster, checkout the awesome
[benchmarks](http://vertxproject.wordpress.com/2012/05/09/vert-x-vs-node-js-simple-http-benchmarks/)

By using Vertx, jubilee inherent advantages in terms of performance, and all
the [other cool features of Vertx](#roadmap).

Installation
------------

    $ jruby -S gem install jubilee

Jubilee requires JRuby 1.7.0 or later, JDK 7+

Get started
-----------

    $ cd a-rack-app
    $ jruby -S jubilee

Setup
-----

If you use bundler, you might want to add `jubilee` to your Gemfile

    $ jubilee

or if you prefer to use the rack handler(e.g. development) use:

    $ rails s jubilee

or

    $ rackup -s jubilee

Event Bus
=========

Event Bus is a pub/sub mechanism, it can be used from server to server, server
to client and client to client, with the same API!

Examples
--------

Start jubilee in a rack application with:

```
$ jubilee --eventbus /eventbus
```

In one browser:

```javascript
JubileeEventBus = new vertx.EventBus("http://localhost:3215/eventbus");
JubileeEventBus.registerHandler("test", function(data){
  console.info(data);
});

```

In another:

```javascript
JubileeEventBus = new vertx.EventBus("http://localhost:3215/eventbus");
JubileeEventBus.send("test", "hello, world");
```

Roadmap
-----------

  Event Bus (wip)

  Shared data

  Clustering

License
--------

The same as JRuby and vertx
