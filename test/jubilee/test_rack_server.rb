require 'test_helper'
require 'rack/lint'
require 'rack/commonlogger'

class TestRackServer < MiniTest::Unit::TestCase
  include Helpers

  def setup
    @valid_request = "GET / HTTP/1.1\r\nHost: test.com\r\nContent-Type: text/plain\r\n\r\n"
    @checker = "checker.ru"
    @host = "localhost"
    @port = 8080
  end

  def teardown
    @server.stop if @server
    sleep 0.1
  end

  def test_lint
    @server = Jubilee::Server.new @checker

    @server.start

    hit(['http://127.0.0.1:8080/test'])

    if exc = @checker.exception
      raise exc
    end
  end

  def test_large_post_body
    @checker = ErrorChecker.new ServerLint.new(@simple)
    @server = Jubilee::Server.new @checker

    @server.start
    sleep 0.5

    big = "x" * (1024 * 16)

    Net::HTTP.post_form URI.parse('http://127.0.0.1:8080/test'),
                 { "big" => big }

    if exc = @checker.exception
      raise exc
    end
  end

  def test_path_info
    input = nil
    @server = Jubilee::Server.new(lambda { |env| input = env; @simple.call(env) })
    @server.start

    hit(['http://127.0.0.1:8080/test/a/b/c'])

    assert_equal "/test/a/b/c", input['PATH_INFO']
  end

  def test_request_method
    input = nil
    @server = Jubilee::Server.new(Rack::MethodOverride.new(lambda { |env| input = env; @simple.call(env) }))
    @server.start

    POST('/test/a/b/c', {"_method" => "delete", "user" => 1})
    assert_equal "DELETE", input['REQUEST_METHOD']

    # it should not memorize env
    POST('/test/a/b/c', {"foo" => "bar"})
    assert_equal "POST", input['REQUEST_METHOD']

  end

  def test_query_string
    input = nil
    @server = Jubilee::Server.new(lambda { |env| input = env; @simple.call(env) })
    @server.start
    sleep 0.1

    hit(['http://127.0.0.1:8080/test/a/b/c?foo=bar'])

    assert_equal "foo=bar", input['QUERY_STRING']
  end

  def test_post_data
    require 'rack/request'
    input = nil
    @server = Jubilee::Server.new(lambda { |env| input = env; @simple.call(env) })
    @server.start
    sleep 0.1

    req = Net::HTTP::Post::Multipart.new("/", "foo" => "bar")
    Net::HTTP.start('localhost', 8080) do |http|
      http.request req
    end

    #Net::HTTP.post_form URI.parse('http://127.0.0.1:8080/test'), { "foo" => "bar" }

    request = Rack::Request.new input
    assert_equal "bar", request.params["foo"]
  end

  def test_end_request_when_rack_crashes
    @server = Jubilee::Server.new(RackCrasher.new(@simple))
    @server.start
    res = hit(['http://127.0.0.1:8080/test'])
    assert_kind_of Net::HTTPServerError, res[0]
  end

  # GH_9
  def test_string_port_value
    @server = Jubilee::Server.new(@simple, {Port: "3000"})
    # assert_wont_raise_anything
    @server.start
  end
end
