require 'test_helper'
require 'jubilee/server'
require 'jubilee/configuration'
require 'net/http'

class TestJubileeServer < MiniTest::Unit::TestCase
  def setup
    @host, @port = "localhost", 3212
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
    config = Jubilee::Configuration.new({rackup: File.join(File.dirname(__FILE__), "../config/app.rb")})
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
end
