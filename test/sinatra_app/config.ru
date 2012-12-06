require './app'
#$:.unshift("/home/isaiah/codes/ruby/rack/lib")
#require 'pp'
#require 'rack/multipart'
#run lambda { |env| pp env; puts "=="; pp env['rack.input'].gets; puts "=="; params = Rack::Multipart.parse_multipart(env); pp params;  [200, {"Content-Type" => "text/plain", "Content-Length" => "44"}, params.map{|k,v| k + ":" + v}] }
run Vertx
