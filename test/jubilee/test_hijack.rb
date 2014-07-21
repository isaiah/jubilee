require 'test_helper'
require 'socket'

class TestJubileeServer < MiniTest::Unit::TestCase
  def setup
  end

  def teardown
    @client.close
    sleep 0.1
    @server.stop
  end

  def test_hijack_supported
    skip "not valid"
    @valid_post = "POST / HTTP/1.1\r\nHost: test.com\r\nContent-Type: text/plain\r\nContent-Length: 5\r\n\r\nhello"
    @host, @port = "localhost", 8080

    config = Jubilee::Configuration.new(rackup: File.expand_path("../../apps/hijack.ru", __FILE__), instances: 1)
    @server = Jubilee::Server.new(config.options)
    @server.start
    @client = TCPSocket.new @host, @port

    @client << @valid_post
    assert @jijack
  end
end
