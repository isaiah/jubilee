require 'sinatra'

class Vertx < Sinatra::Base
  get '/' do
    "<h1>Hello Sinatra!</h1>"
  end

  get '/app' do
    <<EOF
      <h1>Powered by Jubilee</h1>
      <a href="http://www.github.com/isaiah/jubilee">Homepage</a>
      <p>Date: #{Time.now.strftime("%m %d, %Y")}</p>
EOF
  end
end
