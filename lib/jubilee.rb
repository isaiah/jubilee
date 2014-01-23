require File.join(File.dirname(__FILE__), "../jars/jackson-core-2.2.2.jar")
require File.join(File.dirname(__FILE__), "../jars/jackson-databind-2.2.2.jar")
require File.join(File.dirname(__FILE__), "../jars/jackson-annotations-2.2.2.jar")
require File.join(File.dirname(__FILE__), "../jars/hazelcast-2.6.3.jar")
require File.join(File.dirname(__FILE__), "../jars/vertx-core-2.1M3.jar")
require File.join(File.dirname(__FILE__), "../jars/vertx-hazelcast-2.1M3.jar")
require File.join(File.dirname(__FILE__), "../jars/vertx-platform-2.1M3.jar")
require File.join(File.dirname(__FILE__), "../jars/netty-all-4.0.14.Final.jar")

require 'jubilee/jubilee.jar'
require 'rack'
require 'jubilee/version'
require 'jubilee/const'
require 'jubilee/server'
require 'jubilee/application'
require 'jubilee/configuration'
require 'jubilee/response'
require 'rack/handler/jubilee'
require 'rack/chunked'
