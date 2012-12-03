#$:.unshift("/home/isaiah/codes/ruby/rack/lib")
require 'sinatra'

class Vertx < Sinatra::Base
  get '/' do
    "<h1>Hello Sinatra! #{params[:foo]}</h1>"
  end

  post "/home/test" do
    #require 'pp'
    #pp env['rack.input'].gets
    #require 'rack/multipart'
    #puts "=========="
    ##pp Rack::Multipart.parse_multipart(env)
    #puts "=========="
    ##pp env
    #pp request.params
    #puts "=========="
    a = ""
    params.each{|k,v| a << "#{k}:#{v}\n"}
    a
  end

  get '/app' do
    <<EOF
      <h1>Powered by Jubilee</h1>
      <a href="http://www.github.com/isaiah/jubilee">Homepage</a>
      <p>Date: #{Time.now.strftime("%m %d, %Y")}</p>
EOF
  end
end
