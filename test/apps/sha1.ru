bs = 4096
hdr = {'Content-Type' => 'text/plain', 'Content-Length' => '0'}
sha1_app = lambda do |env|
  sha1 = Digest::SHA1.new
  input = env['rack.input']
  resp = {}

  i = 0
  while buf = input.read(bs)
    sha1.update(buf)
    i += buf.size
  end
  resp[:sha1] = sha1.hexdigest

  # rewind and read again
  input.rewind
  sha1.reset

  while buf = input.read(bs)
    sha1.update(buf)
  end

  if resp[:sha1] == sha1.hexdigest
    resp[:sysread_read_byte_match] = true
  end

  if expect_size = env['HTTP_X_EXPECT_SIZE']
    if expect_size.to_i == i
      resp[:expect_size_match] = true
    end
  end
  resp[:size] = i
  resp[:expect_size] = expect_size

  [ 200, hdr.merge({'X-Resp' => resp.inspect}), [] ]
end

run sha1_app
