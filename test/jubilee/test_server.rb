require 'test_helper'
require 'net/http'
require 'open-uri'

class TestJubileeServer < MiniTest::Unit::TestCase
  def setup
    @host, @port = "localhost", 8080
    @server = nil
  end

  def teardown
    @server.stop if @server
  end

  def test_server_lambda
    app = lambda {|env| [200, {"Content-Type" => "text/plain"}, ["http"]] }
    @server = Jubilee::Server.new(app)
    @server.start
    sleep 0.1

    http, body = Net::HTTP.new(@host, @port), nil
    http.start do
      req = Net::HTTP::Get.new "/", {}
      http.request(req) do |resp|
        body = resp.body
      end
    end
    assert_equal "http", body
  end

  def test_server_embedded
    config = Jubilee::Configuration.new(rackup: File.join(File.dirname(__FILE__), "../config/config.ru"))
    @server = Jubilee::Server.new(config.app)
    @server.start
    sleep 0.1
    http, body = Net::HTTP.new(@host, @port), nil
    http.start do
      req = Net::HTTP::Get.new "/", {}
      http.request(req) do |resp|
        body = resp.body
      end
    end
    assert_equal "embedded app", body
  end

  def test_large_post_body
    skip
  end

  def test_url_scheme_for_https
    app = lambda { |env| [200, {}, [env['rack.url_scheme']]] }
    @server = Jubilee::Server.new(app, {port:@port, ssl:true, 
                                 ssl_keystore: File.join(File.dirname(__FILE__), "../../examples/keystore.jks"),
    ssl_password: "hellojubilee"})
    @server.start
    sleep 0.1
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

  def test_port_as_string
    config = Jubilee::Configuration.new(rackup: File.join(File.dirname(__FILE__), "../config/config.ru"))
    @server = Jubilee::Server.new(config.app, Port: @port.to_s)
    @server.start
    sleep 0.1
    open("http://#{@host}:#{@port}/").read
  end
end
