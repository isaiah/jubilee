require './app'
require 'pp'
#run lambda { |env| pp env;  [200, {"Content-Type" => "text/plain", "Content-Length" => "44"}, ["Hello. The time is #{Time.now}"]] }
run Vertx
