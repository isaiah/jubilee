headers = { "X-Header" => "Works" }
body = ["Hello"]

app = lambda do |env|
  status = Integer(env['HTTP_X_STATUS'] || 200)
  [status, headers, body]
end

run app
