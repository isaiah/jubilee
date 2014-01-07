require 'test_helper'
require 'socket'

class TestJubileeServer < MiniTest::Unit::TestCase
  def setup
    @valid_post = "POST / HTTP/1.1\r\nHost: test.com\r\nContent-Type: text/plain\r\nContent-Length: 5\r\n\r\nhello"
    @host, @port = "localhost", 8080
    app = lambda do |env|
      @hijack = env["rack.hijack?"]
      env["rack.hijack"].call
      @io = env["rack.hijack_io"]
      [200, {}, ["hello"]]
    end
    @server = Jubilee::Server.new(app)
    @server.start
    sleep 0.1
    @client = TCPSocket.new @host, @port
  end

  def teardown
    @client.close
    sleep 0.1
    @server.stop
  end

  def test_hijack_supported
    @client << @valid_post
    assert @jijack
  end
end
