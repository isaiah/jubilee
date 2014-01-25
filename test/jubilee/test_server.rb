require 'test_helper'
require 'net/http'

class TestJubileeServer < MiniTest::Unit::TestCase
  def setup
    @host, @port = "localhost", 8080
    @server = nil
  end

  def teardown
    @server.stop if @server
    sleep 0.1
  end

  def test_server_lambda
    app = lambda {|env| [200, {"Content-Type" => "text/plain"}, ["http"]] }
    @server = Jubilee::Server.new(app)
    @server.start
    sleep 0.5

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
    config = Jubilee::Configuration.new(rackup: File.expand_path("../../config/config.ru", __FILE__))
    @server = Jubilee::Server.new(nil, config.options)
    @server.start
    sleep 0.5
    http, body = Net::HTTP.new(@host, @port), nil
    http.start do
      req = Net::HTTP::Get.new "/", {}
      http.request(req) do |resp|
        body = resp.body
      end
    end
    assert_equal "embedded app", body
  end

  def test_url_scheme_for_https
    app = lambda { |env| [200, {}, [env['rack.url_scheme']]] }
    @server = Jubilee::Server.new(app, {port:@port, ssl:true, 
                                 ssl_keystore: File.join(File.dirname(__FILE__), "../../examples/keystore.jks"),
    ssl_password: "hellojubilee"})
    @server.start
    sleep 0.5
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

end
