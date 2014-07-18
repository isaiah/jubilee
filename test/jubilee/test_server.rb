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
    config = Jubilee::Configuration.new(rackup: File.expand_path("../../config/config.ru", __FILE__), instances: 1)
    @server = Jubilee::Server.new(config.options)
    @server.start
    sleep 1
    resp = GET("/")
    assert_equal "embedded app", resp.body
  end

  def test_url_scheme_for_https
    config = Jubilee::Configuration.new(rackup: File.expand_path("../../apps/url_scheme.ru", __FILE__), port: @port, ssl: true,
                                        ssl_keystore: File.join(File.dirname(__FILE__), "../../examples/keystore.jks"),
                                        ssl_password: "hellojubilee", instances: 1)
    @server = Jubilee::Server.new(config.options)
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
