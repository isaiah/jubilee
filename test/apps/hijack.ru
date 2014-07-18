app = lambda do |env|
  io = env["rack.hijack"].call
  io.write "HTTP/1.1 200\r\n\r\nBLAH\n"
  [-1, {}, []]
end

run app
