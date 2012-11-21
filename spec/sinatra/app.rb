require 'sinatra'

class Vertx < Sinatra::Base
  get '/' do
    "<h1>Hello Sinatra!</h1>"
  end
end
