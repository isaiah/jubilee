use Rack::MethodOverride
run lambda { |env| [200, { "X-Header" => "Works" }, [env["REQUEST_METHOD"]]] }
