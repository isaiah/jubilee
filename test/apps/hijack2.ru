# hijack response is not supported by vertx yet
app = lambda do |env|
  body = lambda {|io| io.write "BLAH\n"; io.close }
  [200, {'rack.hijack' => body}, []]
end

run app
