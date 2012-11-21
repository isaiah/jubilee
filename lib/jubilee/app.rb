class App
  def call(env)
    puts env['RACK_INPUT']
    body  = "<h1>hello world</h1>"
    [200, {"Content-Type" => "text/html", "Content-Length" => body.length}, [body]]
  end
end
