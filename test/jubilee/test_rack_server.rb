require 'test_helper'
require 'json'

class TestRackServer < MiniTest::Unit::TestCase
  include Helpers

  def teardown
    @server.stop
    sleep 0.1
  end

  def test_lint
    start_server("checker")
    resp = hit(['http://127.0.0.1:8080/test']).first

    if exc = JSON.parse(resp.body)["exception"]
      raise exc
    end
  end

  def test_large_post_body
    start_server("checker")
    big = "x" * (1024 * 16)
    resp = POST('/test', { "big" => big })
    if exc = JSON.parse(resp.body)["exception"]
      raise exc
    end
  end

  def test_path_info
    start_server("simple")
    resp = hit(['http://127.0.0.1:8080/test/a/b/c']).first
    assert_equal "/test/a/b/c", JSON.parse(resp.body)['PATH_INFO']
  end

  def test_request_method
    start_server("method_override")
    resp = POST('/test/a/b/c', {"_method" => "delete", "user" => 1})
    assert_equal "DELETE", resp.body

    # it should not memorize env
    resp = POST('/test/a/b/c', {"foo" => "bar"})
    assert_equal "POST", resp.body
  end

  def test_query_string
    start_server("simple")
    resp = hit(['http://127.0.0.1:8080/test/a/b/c?foo=bar']).first
    assert_equal "foo=bar", JSON.parse(resp.body)['QUERY_STRING']
  end

  def test_post_data
    require 'rack/request'
    start_server("simple")
    req = Net::HTTP::Post::Multipart.new("/", "foo" => "bar")
    resp = Net::HTTP.start('localhost', 8080) do |http|
      http.request req
    end

    assert_equal "bar", JSON.parse(resp.body)["foo"]
  end

  def test_end_request_when_rack_crashes
    start_server("rack_crasher")
    res = hit(['http://127.0.0.1:8080/test'])
    assert_kind_of Net::HTTPServerError, res[0]
  end

  def start_server(ru, &block)
    config = Jubilee::Configuration.new(rackup: File.expand_path("../../apps/#{ru}.ru", __FILE__), instances: 1)
    @server = Jubilee::Server.new(config.options)
    q = Queue.new
    @server.start{ q << 1 }
    q.pop
  end
end
