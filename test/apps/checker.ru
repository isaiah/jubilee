require 'rack'
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

use ServerLint
use ErrorChecker
run lambda { |env| [200, { "X-Header" => "Works" }, ["Hello"]] }
