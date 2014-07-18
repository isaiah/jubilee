hdr = {'Content-Type' => 'text/plain', 'Content-Length' => '0'}
app = lambda do |env|
  nr = 0
  while buf = env['rack.input'].read(65536)
    nr += buf.size
  end
  [ 200, hdr.merge('X-Read-Length' => nr.to_s), [] ]
end
run app
