require 'rack/request'
require 'json'

app = lambda do |env|
  req = Rack::Request.new(env)
  body = JSON.dump(req.params.merge({"QUERY_STRING" => env["QUERY_STRING"], "PATH_INFO" => env["PATH_INFO"]}))
  [200, { "X-Header" => "Works" }, [body]]
end

run app
