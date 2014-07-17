hdr = {'Content-Type' => 'text/plain', 'Content-Length' => '0'}
run lambda do |env|
  nr = 0
  while buf = env['rack.input'].read(65536)
    nr += buf.size
  end
  [ 200, hdr.merge('HTTP-X-Read-Length' => nr.length), [] ]
end
