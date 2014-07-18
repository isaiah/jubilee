app = lambda do |env|
  hijack = env["rack.hijack?"]
  env["rack.hijack"].call
  io = env["rack.hijack_io"]
  [200, {}, ["hello"]]
end

run app
