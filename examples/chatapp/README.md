Get started
-----------

There is a [online demo](http://192.241.201.68:8080/) for this
application.

Make sure you are using jruby 1.7+ (jubilee 1.1.0+ require jruby 1.7.5 or later) and your JDK version is 7+

To run the application:

```shell
bundle && jubilee --eventbus /eventbus
```

Then go to http://localhost:8080 in your browser, if you have the page
opened in multiple tabs or windows you can see the message you sent are
broadcasted to all the other tabs.
