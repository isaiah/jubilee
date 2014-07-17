run lambda { |env| [200, {}, [env['rack.url_scheme']]] }
