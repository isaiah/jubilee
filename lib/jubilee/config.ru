run lambda { |env| [200, {"Content-Type" => "text/plain", "Content-Length" => 44}, ["Hello. The time is #{Time.now}"]] }
