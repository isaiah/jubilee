[![Build Status](https://travis-ci.org/isaiah/jubilee.png?branch=master)](https://travis-ci.org/isaiah/jubilee)

Jubilee server
=========
 
A rack application compatible http server built on Vertx3. Check out the
demo [application](https://github.com/isaiah/jubilee/tree/master/examples/chatapp).

Why another rack server?
------------------------

> "Vert.x is a lightweight, high performance application platform for the JVM
> that's designed for modern mobile, web, and enterprise applications."
>      - vertx.io site

By using Vertx, jubilee inherent advantages in terms of performance, and all
the other cool features of Vertx:

* [EventBus](https://github.com/isaiah/jubilee/wiki/Event-Bus)
* [SharedData](https://github.com/isaiah/jubilee/wiki/SharedData)
* [Clustering](https://github.com/isaiah/jubilee/wiki/Clustering)



Get started
------------

Make sure you have JDK 8 and jruby 1.7.20+ installed.

```shell
bundle && bundle exec rake install
```

This is the development branch of jubilee, it uses the vertx
3.0 for a working version, please check out the
[2.x branch](https://github.com/isaiah/jubilee/tree/2.x).

Rails
-----

Under the default setup, jubilee runs 4 instances of web
servers, each with it's own jruby runtime, if you find that jubilee
crashes or hangs with OutOfMemeoryError, please tune your JVM OPTS
like this:

    $ export JAVA_OPTS="-Xms1024m -Xmx2048m -XX:PermSize=512m -XX:MaxPermSize=512m"

If your OS memory is quite limited, please run jubilee with

    $ jubilee -n 1

Event Bus
=========

Event Bus is a pub/sub mechanism, it can be used from server to server, server
to client and client to client, with the same API! You can use it to build
living real time web application.

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

In the previous tab it should print the greetings you just sent.

For more advanced examples, please checkout the
[chatapp](https://github.com/isaiah/jubilee/tree/master/examples/chatapp).

Performance Tuning
-------------------

If you're creating a lot of connections to a Jubilee(Vert.x) server in a short
period of time, e.g. benchmarking with tools like [wrk](https://github.com/wg/wrk),
you may need to tweak some settings in order to avoid the TCP accept queue
getting full. This can result in connections being refused or packets being
dropped during the handshake which can then cause the client to retry.

A classic symptom of this is if you see long connection times just over
3000ms at your client.

How to tune this is operating system specific but in Linux you need to
increase a couple of settings in the TCP / Net config (10000 is an
arbitrarily large number)

```shell
sudo sysctl -w net.core.somaxconn=10000
sudo sysctl -w net.ipv4.tcp_max_syn_backlog=10000
```

For other operating systems, please consult your operating system
documentation.

Contributing
-------------

All kinds of contributions are welcome.

File an issue [here](https://github.com/isaiah/jubilee/issues) if you encounter any problems. Or if you prefer to fix by yourself:

1. Fork it
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request

License
--------

See [LICENSE.txt](https://github.com/isaiah/jubilee/blob/master/LICENSE.txt)

Acknowledgment
--------------

YourKit is kindly supporting Jubilee Server with its full-featured Java Profiler.
YourKit, LLC is the creator of innovative and intelligent tools for profiling
Java and .NET applications. Take a look at YourKit's leading software products:
[YourKit Java
Profiler](http://www.yourkit.com/java/profiler/index.jsp) and
[YourKit .NET Profiler](http://www.yourkit.com/.net/profiler/index.jsp).
