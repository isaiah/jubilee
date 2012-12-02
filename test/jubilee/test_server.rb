require 'test_helper'
require 'net/http'

class TestJubileeServer < MiniTest::Unit::TestCase
  def setup
    @host, @port = "localhost", 3215
  end

  def test_server_lambda
    app = lambda {|env| [200, {"Content-Type" => "text/plain"}, ["http"]] }
    server = Jubilee::Server.new(app)
    server.start

    http, body = Net::HTTP.new(@host, @port), nil
    http.start do
      req = Net::HTTP::Get.new "/", {}
      http.request(req) do |resp|
        body = resp.body
      end
    end
    server.stop
    assert_equal "http", body
  end

  def test_server_embeded
    config = Jubilee::Configuration.new(rackup: File.join(File.dirname(__FILE__), "../config/app.rb"))
    config.load
    server = Jubilee::Server.new(config.app)
    server.start
    http, body = Net::HTTP.new(@host, @port), nil
    http.start do
      req = Net::HTTP::Get.new "/", {}
      http.request(req) do |resp|
        body = resp.body
      end
    end
    server.stop
    assert_equal "embeded app", body
  end

  def test_large_post_body
    skip
  end

  
  def test_url_scheme_for_https
    app = lambda { |env| [200, {}, [env['rack.url_scheme']]] }
    server = Jubilee::Server.new(app, {port:@port, ssl:true, 
                                 keystore: File.join(File.dirname(__FILE__), "../../examples/jubilee/server-keystore.jks"),
    keystore_password: "wibble"})
    server.start
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

    server.stop
    assert_equal "https", body
  end
end
