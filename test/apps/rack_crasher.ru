class RackCrasher < Rack::Lint
  def call(env)
    raise "Oops"
  end
end

use RackCrasher
run lambda { |env| [200, { "X-Header" => "Works" }, ["Hello"]] }
