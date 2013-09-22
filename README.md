[![Build Status](https://travis-ci.org/isaiah/jubilee.png?branch=master)](https://travis-ci.org/isaiah/jubilee)

Jubilee
=========
 
> "We need a web framework for Vertx.", you said.

> "But why not use Vertx in your Rails applications, it's the most productive web framework ever created."

The Answer is Jubilee, an rack server with [vertx 2.0](http://vertx.io) awesomeness builtin.

Why another rack server?
------------------------

> "Vert.x is a lightweight, high performance application platform for the JVM
> that's designed for modern mobile, web, and enterprise applications."
>      - vertx.io site

In short, Vertx is nodejs on the JVM, only much more faster, checkout the awesome
[benchmarks](http://vertxproject.wordpress.com/2012/05/09/vert-x-vs-node-js-simple-http-benchmarks/)

By using Vertx, jubilee inherent advantages in terms of performance, and all
the other cool features of Vertx:

* [EventBus](https://github.com/isaiah/jubilee/wiki/Event-Bus)
* [SharedData](https://github.com/isaiah/jubilee/wiki/SharedData)
* [Clustering](https://github.com/isaiah/jubilee/wiki/Clustering)



Installation
------------

    $ jruby -S gem install jubilee --pre

Jubilee requires JRuby 1.7.0 or later, and JDK 7+

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

Assume necessary javascript files are loaded in the page (they can be found [here](https://github.com/isaiah/jubilee/tree/master/examples/client)),
start jubilee in a rack application with:

```
$ jubilee --eventbus /eventbus
```

In one browser:

```javascript
var eb = new vertx.EventBus("/eventbus");
eb.registerHandler("test", function(data){
  console.info(data);
});

```

In another:

```javascript
var eb = new vertx.EventBus("/eventbus");
eb.send("test", "hello, world");
```

For more advanced examples, checkout the
[chatapp](https://github.com/isaiah/jubilee/tree/master/examples/chatapp).

License
--------

The same as JRuby and vertx
