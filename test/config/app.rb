class App
  def call(env)
    [200, {"Content-Type" => "text/plain"}, ["embeded app"]]
  end
end
