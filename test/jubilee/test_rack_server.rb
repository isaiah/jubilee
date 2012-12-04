require 'test_helper'
require 'rack/lint'
require 'rack/commonlogger'

class TestRackServer < MiniTest::Unit::TestCase

  class ErrorChecker
    def initialize(app)
      @app = app
      @exception = nil
      @env = nil
    end

    attr_reader :exception, :env

    def call(env)
      begin
        @env = env
        return @app.call(env)
      rescue Exception => e
        @exception = e

        [
          500,
          { "X-Exception" => e.message, "X-Exception-Class" => e.class.to_s },
          ["Error detected"]
        ]
      end
    end
  end

  class ServerLint < Rack::Lint
    def call(env)
      assert("No env given") { env }
      check_env env

      @app.call(env)
    end
  end

  def setup
    @valid_request = "GET / HTTP/1.1\r\nHost: test.com\r\nContent-Type: text/plain\r\n\r\n"
    
    @simple = lambda { |env| [200, { "X-Header" => "Works" }, ["Hello"]] }
    @checker = ErrorChecker.new ServerLint.new(@simple)
  end

  def teardown
    @server.stop
  end

  def test_lint
    @server = Jubilee::Server.new @checker

    @server.start

    hit(['http://127.0.0.1:3215/test'])

    if exc = @checker.exception
      raise exc
    end
  end

  def test_large_post_body
    @checker = ErrorChecker.new ServerLint.new(@simple)
    @server = Jubilee::Server.new @checker

    @server.start

    big = "x" * (1024 * 16)

    Net::HTTP.post_form URI.parse('http://127.0.0.1:3215/test'),
                 { "big" => big }

    if exc = @checker.exception
      raise exc
    end
  end

  def test_path_info
    input = nil
    @server = Jubilee::Server.new (lambda { |env| input = env; @simple.call(env) })
    @server.start

    hit(['http://127.0.0.1:3215/test/a/b/c'])

    assert_equal "/test/a/b/c", input['PATH_INFO']
  end

  def test_query_string
    input = nil
    @server = Jubilee::Server.new (lambda { |env| input = env; @simple.call(env) })
    @server.start

    hit(['http://127.0.0.1:3215/test/a/b/c?foo=bar'])

    assert_equal "foo=bar", input['QUERY_STRING']
  end

  def test_post_data
    require 'rack/request'
    input = nil
    @server = Jubilee::Server.new (lambda { |env| input = env; @simple.call(env) })
    @server.start

    req = Net::HTTP::Post::Multipart.new("/", "foo" => "bar")
    resp = Net::HTTP.start('localhost', 3215) do |http|
      http.request req
    end

    #Net::HTTP.post_form URI.parse('http://127.0.0.1:3215/test'), { "foo" => "bar" }

    require 'pp'
    request = Rack::Request.new input
    puts "======="
    pp request.params
    puts "======="
    assert_equal "bar", request.params["foo"]
  end
end
