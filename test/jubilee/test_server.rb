require 'test_helper'
require 'net/http'

class TestJubileeServer < MiniTest::Unit::TestCase
  include Helpers
  def setup
    @host, @port = "localhost", 8080
    @server = nil
  end

  def teardown
    @server.stop if @server
    sleep 0.1
  end

  def test_server_embedded
    start_server('config')
    resp = GET("/")
    assert_equal "embedded app", resp.body
  end

  def test_url_scheme_for_https
    config = Jubilee::Configuration.new(rackup: File.expand_path("../../apps/url_scheme.ru", __FILE__), port: @port, ssl: true,
                                        ssl_keystore: File.join(File.dirname(__FILE__), "../../examples/keystore.jks"),
                                        ssl_password: "hellojubilee", instances: 1)
    @server = Jubilee::Server.new(config.options)
    q = Queue.new
    @server.start{ q << 1 }
    q.pop
    http = Net::HTTP.new @host, @port
    http.use_ssl = true
    http.verify_mode = OpenSSL::SSL::VERIFY_NONE

    body = nil
    http.start do
      req = Net::HTTP::Get.new "/", {}

      http.request(req) do |rep|
        body = rep.body
      end
    end

    assert_equal "https", body
  end

  def test_proper_rack_input_io
    start_server('rack_input')
    fifteen = "1" * 15

    sock = TCPSocket.new @host, @port
    sock << "PUT / HTTP/1.0\r\nContent-Length: 30\r\n\r\n#{fifteen}"
    sleep 0.1 # important so that the previous data is sent as a packet
    sock << fifteen

    while true
      line = sock.gets
      break if line == "\r\n"
    end
    data = sock.read
    assert_equal "#{fifteen}#{fifteen}", data
  end

  def test_huge_return
    start_server('huge')
    sock = TCPSocket.new @host, @port
    sock << "GET / HTTP/1.0\r\n\r\n"

    while true
      line = sock.gets
      break if line == "\r\n"
    end

    out = sock.read

    giant = 2056610
    assert_equal giant, out.bytesize
  end

  def start_server(rack, &block)
    config = Jubilee::Configuration.new(rackup: File.expand_path("../../apps/#{rack}.ru", __FILE__), instances: 1)
    @server = Jubilee::Server.new(config.options)
    q = Queue.new
    @server.start { q << 1 }
    q.pop
    sleep 0.1
  end
end
