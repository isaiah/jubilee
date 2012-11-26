require 'test_helper'
require 'jubilee/server'
require 'net/http'

class TestJubileeServer < MiniTest::Unit::TestCase
  def setup
    @host, @port = "localhost", 3212
  end

  def test_server_lambda
    server = Jubilee::Server.new.request_handler(1) do
      run lambda {|env| [200, {"Content-Type" => "text/plain"}, ["http"]] }
    end

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
    server = Jubilee::Server.new.request_handler({rackup: File.join(File.dirname(__FILE__), "../config/app.rb")})
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
end
