require 'rack/lint'
require 'json'
class ErrorChecker
  def initialize(app)
    @app = app
  end

  def call(env)
    begin
      return @app.call(env)
    rescue Exception => e
      [
        500,
        { "X-Exception" => e.message, "X-Exception-Class" => e.class.to_s },
        [JSON.dump({"exception" => e.message})]
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
app = lambda { |env| [200, { "X-Header" => "Works" }, [JSON.dump({r:'Hello'})]] }
run app
