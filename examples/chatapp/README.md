Get started
-----------

There is a [online demo](http://192.241.201.68:8080/) for this
application.

Make sure you are using jruby 1.7+ (jubilee 1.1.0+ require jruby 1.7.5 or later) and your JDK version is 7+

There are two ways to run this application:

1. Use jubilee server

```shell
bundle && jubilee --eventbus /eventbus
```

2. Run in vertx

Install the lastest stable version of vertx(2.1.2), and put the following snippet in your vertx
language configuration, you can find the file under $VERTX\_HOME/conf/langs.properties

```
rackup=isaiah~mod-rack~0.1.1:org.jruby.jubilee.JubileeVerticleFactory
.ru=rackup
```

and run

```shell
vertx run config.ru -conf config.json
```

Then go to http://localhost:8080 in your browser, if you have the page
opened in multiple tabs or windows you can see the message you sent are
broadcasted to all the other tabs.
