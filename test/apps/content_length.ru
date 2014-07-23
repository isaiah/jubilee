require_relative './persistent'

run Persistent.new(["hello", " world"], {"X-Header" => "Works", "Content-Length" => 11})
