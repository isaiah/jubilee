require 'test_helper'
require 'jubilee/server'
require 'net/http'

class TestJubileeServer < MiniTest::Unit::TestCase
  def setup
    @host, @port = "localhost", 3212
  end

  def test_server_lambda
    Jubilee::Server.new.request_handler(1) do
      run lambda {|env| [200, {"Content-Type" => "text/plain", "Content-Length" => "5"}, ["http"]] }
    end

    http, body = Net::HTTP.new(@host, @port), nil
    http.start do
      req = Net::HTTP::Get.new "/", {}
      http.request(req) do |resp|
        body = resp.body
      end
    end
    assert_equal "http", body
  end
end
