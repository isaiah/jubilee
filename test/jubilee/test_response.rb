require 'test_helper'
require 'timeout'
require 'socket'
class TestResponse < MiniTest::Unit::TestCase
  def setup
    @valid_request = "GET / HTTP/1.1\r\nHost: test.com\r\nContent-Type: text/plain\r\n\r\n"
    @close_request = "GET / HTTP/1.1\r\nHost: test.com\r\nContent-Type: text/plain\r\nConnection: Close\r\n\r\n"
    @http10_request = "GET / HTTP/1.0\r\nHost: test.com\r\nContent-Type: text/plain\r\n\r\n"
    @keep_request = "GET / HTTP/1.0\r\nHost: test.com\r\nContent-Type: text/plain\r\nConnection: Keep-Alive\r\n\r\n"

    @valid_post = "POST / HTTP/1.1\r\nHost: test.com\r\nContent-Type: text/plain\r\nContent-Length: 5\r\n\r\nhello"
    @valid_no_body = "GET / HTTP/1.1\r\nHost: test.com\r\nX-Status: 204\r\nContent-Type: text/plain\r\n\r\n"

    @headers = { "X-Header" => "Works" }
    @body = ["Hello"]
    @inputs = []

    @simple = lambda do |env|
      @inputs << env['rack.input']
      status = Integer(env['HTTP_X_STATUS'] || 200)
      [status, @headers, @body]
    end

    @host = "127.0.0.1"
    @port = 8080

    @server = Jubilee::Server.new @simple
    @server.start
    sleep 0.1
    @client = TCPSocket.new @host, @port
  end

  def teardown
    @client.close
    sleep 0.1 # in case server shutdown before request is submitted
    @server.stop
  end

  def lines(count, s=@client)
    str = ""
    timeout(5) do
      count.times { str << s.gets }
    end
    str
  end

  def valid_response(size)
    Regexp.new("HTTP/1.1 200 OK\r\nX-Header: Works\r\n(.*?\r\n)*?Content-Length: #{size}\r\nConnection: keep-alive\r\nDate(.*?)\r\n\r\n", true)
  end

  def test_one_with_content_length
    @client << @valid_request
    sz = @body[0].size.to_s

    assert_match valid_response(sz), lines(7)
    assert_equal "Hello", @client.read(5)
  end

  def test_two_back_to_back
    @client << @valid_request
    sz = @body[0].size.to_s

    assert_match valid_response(sz), lines(7)
    assert_equal "Hello", @client.read(5)

    @client << @valid_request
    sz = @body[0].size.to_s

    assert_match valid_response(sz), lines(7)
    assert_equal "Hello", @client.read(5)
  end

  def test_post_then_get
    @client << @valid_post
    sz = @body[0].size.to_s

    assert_match valid_response(sz), lines(7)
    assert_equal "Hello", @client.read(5)

    @client << @valid_request
    sz = @body[0].size.to_s

    assert_match valid_response(sz), lines(7)
    assert_equal "Hello", @client.read(5)
  end

=begin
  def test_no_body_then_get
    @client << @valid_no_body
    assert_match %r{HTTP/1.1 204 No Content\r\nX-Header: Works(.*?\r\n)*?Connection: keep-alive\r\n\r\n}, lines(6)

    @client << @valid_request
    sz = @body[0].size.to_s

    assert_match %r{HTTP/1.1 200 OK\r\nX-Header: Works\r\n(.*?\r\n)*?Content-Length: #{sz}\r\nConnection: keep-alive\r\n\r\n}, lines(7)
    assert_equal "Hello", @client.read(5)
  end
=end

  def test_chunked
    @body << "Chunked"

    @client << @valid_request

    assert_match %r{HTTP/1.1 200 OK\r\nX-Header: Works\r\nServer(.*\r\n)*?Transfer-Encoding: chunked\r\nDate(.*?)\r\n\r\n5\r\nHello\r\n7\r\nChunked\r\n0\r\n\r\n}, lines(13)
  end

  def test_no_chunked_in_http10
    @body << "Chunked"

    @client << @http10_request

    assert_match %r{HTTP/1.0 200 OK\r\nX-Header: Works(.*?\r\n)*?Connection: close\r\nDate(.*?)\r\n\r\n}, lines(6)
    assert_equal "HelloChunked", @client.read
  end

  def test_hex
    str = "This is longer and will be in hex"
    @body << str

    @client << @valid_request

    assert_match %r{HTTP/1.1 200 OK\r\nX-Header: Works\r\n(.*?\r\n)*?Transfer-Encoding: chunked\r\nDate(.*?)\r\n\r\n5\r\nHello\r\n#{str.size.to_s(16)}\r\n#{str}\r\n0\r\n\r\n}, lines(13)

  end

  def test_client11_close
    @client << @close_request
    sz = @body[0].size.to_s

    assert_match %r{HTTP/1.1 200 OK\r\nX-Header: Works\r\n(.*?\r\n)*?Content-Length: #{sz}\r\nConnection: close\r\nDate(.*?)\r\n\r\n}, lines(7)
    assert_equal "Hello", @client.read(5)
  end

  def test_client10_close
    @client << @http10_request
    sz = @body[0].size.to_s

    assert_match %r{HTTP/1.0 200 OK\r\nX-Header: Works\r\n(.*?\r\n)*?Content-Length: #{sz}\r\nConnection: close\r\nDate(.*?)\r\n\r\n}, lines(7)
    assert_equal "Hello", @client.read(5)
  end

  def test_one_with_keep_alive_header
    @client << @keep_request
    sz = @body[0].size.to_s

    assert_match %r{HTTP/1.0 200 OK\r\nX-Header: Works\r\nServer(.*?\r\n)*?Content-Length: #{sz}\r\nConnection: keep-alive\r\nDate(.*?)\r\n\r\n}, lines(7)
    assert_equal "Hello", @client.read(5)
  end

=begin
  def test_persistent_timeout
    @server.persistent_timeout = 2
    @client << @valid_request
    sz = @body[0].size.to_s

    assert_equal "HTTP/1.1 200 OK\r\nX-Header: Works\r\nContent-Length: #{sz}\r\n\r\n", lines(4)
    assert_equal "Hello", @client.read(5)

    sleep 3

    assert_raises EOFError do
      @client.read_nonblock(1)
    end
  end
=end

  def test_app_sets_content_length
    @body = ["hello", " world"]
    @headers['Content-Length'] = "11"

    @client << @valid_request

    assert_match %r{HTTP/1.1 200 OK\r\nX-Header: Works\r\n(.*?\r\n)*?Content-Length: 11\r\nConnection: keep-alive\r\nDate(.*?)\r\n\r\n}, lines(7)
    assert_equal "hello world", @client.read(11)
  end

  def test_allow_app_to_chunk_itself
    skip "body should not be chunked before sent to jubilee"
    @headers = {'Transfer-Encoding' => "chunked" }

    @body = ["5\r\nhello\r\n0\r\n\r\n"]

    @client << @valid_request

    assert_match %r{HTTP/1.1 200 OK\r\n(.*?\r\n)*?Transfer-Encoding: chunked\r\nDate(.*?)\r\n\r\n5\r\nhello\r\n0\r\n}, lines(10)
  end


  def test_two_requests_in_one_chunk

    req = @valid_request.to_s
    req << "GET /second HTTP/1.1\r\nHost: test.com\r\nContent-Type: text/plain\r\n\r\n"

    @client << req

    sz = @body[0].size.to_s

    assert_match %r{HTTP/1.1 200 OK\r\nX-Header: Works\r\n(.*?\r\n)*?Content-Length: #{sz}\r\nConnection: keep-alive\r\nDate(.*?)\r\n\r\n}, lines(7)
    assert_equal "Hello", @client.read(5)

    assert_match %r{HTTP/1.1 200 OK\r\nX-Header: Works\r\n(.*?\r\n)*?Content-Length: #{sz}\r\nConnection: keep-alive\r\nDate(.*?)\r\n\r\n}, lines(7)
    assert_equal "Hello", @client.read(5)
  end

  def test_second_request_not_in_first_req_body

    req = @valid_request.to_s
    req << "GET /second HTTP/1.1\r\nHost: test.com\r\nContent-Type: text/plain\r\n\r\n"

    @client << req

    sz = @body[0].size.to_s

    assert_match %r{HTTP/1.1 200 OK\r\nX-Header: Works\r\n(.*?\r\n)*?Content-Length: #{sz}\r\nConnection: keep-alive\r\nDate(.*?)\r\n\r\n}, lines(7)
    assert_equal "Hello", @client.read(5)

    assert_match %r{HTTP/1.1 200 OK\r\nX-Header: Works\r\n(.*?\r\n)*?Content-Length: #{sz}\r\nConnection: keep-alive\r\nDate(.*?)\r\n\r\n}, lines(7)
    assert_equal "Hello", @client.read(5)

    #assert_kind_of Jubilee::NullIO, @inputs[0]
    #assert_kind_of Jubilee::NullIO, @inputs[1]
  end

  def test_keepalive_doesnt_starve_clients
    sz = @body[0].size.to_s

    @client << @valid_request

    c2 = TCPSocket.new @host, @port
    c2 << @valid_request

    out = IO.select([c2], nil, nil, 1)

    assert out, "select returned nil"
    assert_equal c2, out.first.first

    assert_match %r{HTTP/1.1 200 OK\r\nX-Header: Works\r\n(.*?\r\n)*?Content-Length: #{sz}\r\nConnection: keep-alive\r\nDate(.*?)\r\n\r\n}, lines(7, c2)
    assert_equal "Hello", c2.read(5)
  end

=begin
  def test_client_shutdown_writes
    bs = 15609315 * rand
    sock = TCPSocket.new('127.0.0.1', @port)
    sock.syswrite("PUT /hello HTTP/1.1\r\n")
    sock.syswrite("Host: example.com\r\n")
    sock.syswrite("Transfer-Encoding: chunked\r\n")
    sock.syswrite("Trailer: X-Foo\r\n")
    sock.syswrite("\r\n")
    sock.syswrite("%x\r\n" % [ bs ])
    sock.syswrite("F" * bs)
    sock.syswrite("\r\n0\r\nX-")
    "Foo: bar\r\n\r\n".each_byte do |x|
      sock.syswrite x.chr
      sleep 0.05
    end
    # we wrote the entire request before shutting down, server should
    # continue to process our request and never hit EOFError on our sock
    sock.shutdown(Socket::SHUT_WR)
    buf = sock.read
    assert_equal 'Hello', buf.split(/\r\n\r\n/).last
    next_client = Net::HTTP.get(URI.parse("http://127.0.0.1:#@port/"))
    assert_equal 'Hello', next_client
    lines = File.readlines("test_stderr.#$$.log")
    assert lines.grep(/^Unicorn::ClientShutdown: /).empty?
    assert_nil sock.close
  end
=end
end
