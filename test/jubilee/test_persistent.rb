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
    @sz = @body[0].size.to_s

    @host = "127.0.0.1"
    @port = 8080

  end

  def teardown
    @client.close if @client
    @server.stop if @server
    sleep 0.1
  end

  def lines(count, s=@client)
    str = ""
    timeout(5) do
      count.times { str << s.gets }
    end
    str
  end

  def valid_response(size = @sz, close = false)
    conn = close ? "close" : "keep-alive"
    %r{HTTP/1.1 200 OK\r\nX-Header: Works\r\nServer: Jubilee\(\d\.\d\.\d\)\r\nContent-Length: #{size}\r\nConnection: #{conn}\r\nDate: (.*?)\r\n\r\n}
  end

  def test_one_with_content_length
    start_server
    @client << @valid_request
    sz = @body[0].size.to_s

    assert_match valid_response(sz), lines(7)
    assert_equal "Hello", @client.read(5)
  end

  def test_two_back_to_back
    start_server
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
    start_server
    @client << @valid_post
    sz = @body[0].size.to_s

    assert_match valid_response(sz), lines(7)
    assert_equal "Hello", @client.read(5)

    @client << @valid_request
    sz = @body[0].size.to_s

    assert_match valid_response(sz), lines(7)
    assert_equal "Hello", @client.read(5)
  end

  def test_no_body_then_get
    start_server
    @client << @valid_no_body
    assert_match %r{HTTP/1.1 204 No Content\r\nX-Header: Works\r\nConnection: keep-alive\r\n(.*?\r\n)*?\r\n}, lines(6)

    @client << @valid_request
    sz = @body[0].size.to_s

    assert_match %r{HTTP/1.1 200 OK\r\nX-Header: Works\r\nServer: Jubilee\(\d\.\d\.\d\)\r\nContent-Length: #{sz}\r\nConnection: keep-alive\r\n(.*?\r\n)*?\r\n}, lines(7)
    assert_equal "Hello", @client.read(5)
  end

  def test_chunked
    start_server("chunked")

    @client << @valid_request

    assert_match %r{HTTP/1.1 200 OK\r\nX-Header: Works\r\nServer: Jubilee\(\d\.\d\.\d\)\r\nConnection: keep-alive\r\nTransfer-Encoding: chunked\r\nDate(.*?)\r\n\r\n5\r\nHello\r\n7\r\nChunked\r\n0\r\n\r\n}, lines(13)
  end

  def test_no_chunked_in_http10
    start_server("chunked")

    @client << @http10_request

    assert_match %r{HTTP/1.0 200 OK\r\nX-Header: Works\r\nServer: Jubilee\(\d\.\d\.\d\)\r\nConnection: close\r\nDate: (.*?)\r\n\r\n}, lines(6)
    assert_equal "HelloChunked", @client.read
  end

  def test_hex
    start_server("hex")
    str = "This is longer and will be in hex"

    @client << @valid_request

    assert_match %r{HTTP/1.1 200 OK\r\nX-Header: Works\r\nServer: Jubilee(.*?)\r\nConnection: keep-alive\r\nTransfer-Encoding: chunked\r\nDate: (.*?)\r\n\r\n5\r\nHello\r\n#{str.size.to_s(16)}\r\n#{str}\r\n0\r\n\r\n}, lines(13)

  end

  def test_client11_close
    start_server
    @client << @close_request
    sz = @body[0].size.to_s

    assert_match valid_response(sz, true), lines(7)
    assert_equal "Hello", @client.read(5)
  end

  def test_app_sets_content_length
    start_server("content_length")
    @client << @valid_request

    assert_match valid_response(11), lines(7)
    assert_equal "hello world", @client.read(11)
  end

  def test_allow_app_to_chunk_itself
    start_server("self_chunked")
    @client << @valid_request

    assert_match %r{HTTP/1.1 200 OK\r\nX-Header: Works\r\nServer: Jubilee(.*?)\r\nConnection: keep-alive\r\nTransfer-Encoding: chunked\r\nDate: (.*?)\r\n\r\n5\r\nhello\r\n0\r\n\r\n}, lines(11)
  end


  def test_two_requests_in_one_chunk
    start_server
    req = @valid_request.to_s
    req << "GET /second HTTP/1.1\r\nHost: test.com\r\nContent-Type: text/plain\r\n\r\n"

    @client << req

    sz = @body[0].size.to_s

    assert_match valid_response(sz), lines(7)
    assert_equal "Hello", @client.read(5)

    assert_match valid_response(sz), lines(7)
    assert_equal "Hello", @client.read(5)
  end

  def test_second_request_not_in_first_req_body
    start_server

    req = @valid_request.to_s
    req << "GET /second HTTP/1.1\r\nHost: test.com\r\nContent-Type: text/plain\r\n\r\n"

    @client << req

    sz = @body[0].size.to_s

    assert_match valid_response(sz), lines(7)
    assert_equal "Hello", @client.read(5)

    assert_match valid_response(sz), lines(7)
    assert_equal "Hello", @client.read(5)

    #assert_kind_of Jubilee::IORackInput, @inputs[0]
    #assert_kind_of Jubilee::IORackInput, @inputs[1]
  end

  def test_keepalive_doesnt_starve_clients
    start_server
    sz = @body[0].size.to_s

    @client << @valid_request

    c2 = TCPSocket.new @host, @port
    c2 << @valid_request

    out = IO.select([c2], nil, nil, 1)

    assert out, "select returned nil"
    assert_equal c2, out.first.first

    assert_match valid_response(sz), lines(7, c2)
    assert_equal "Hello", c2.read(5)
  end

  def start_server(ru='persistent')
    config = Jubilee::Configuration.new(rackup: File.expand_path("../../apps/#{ru}.ru", __FILE__), instances: 1)
    @server = Jubilee::Server.new(config.options)
    q = Queue.new
    @server.start{ q << 1 }
    q.pop
    sleep 0.1
    @client = TCPSocket.new @host, @port
  end
end
