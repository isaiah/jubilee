require 'stringio'

module Jubilee
  class Server
    include Const

    def initialize(*args, &block)
      @proto_env = {
        "rack.version".freeze => Rack::VERSION,
        "rack.errors".freeze => StringIO.new,
        "rack.multithread".freeze => true,
        "rack.multiprocess".freeze => false,
        "rack.run_once".freeze => true,
        # FIXME hardcoded
        "rack.url_scheme".freeze => "http",
        "SCRIPT_NAME".freeze => "",

        # Rack blows up if this is an empty string, and Rack::Lint
        # blows up if it's nil. So 'text/plain' seems like the most
        # sensible default value.
        "CONTENT_TYPE".freeze => "text/plain",

        "QUERY_STRING".freeze => "",
        SERVER_PROTOCOL => HTTP_11,
        SERVER_SOFTWARE => JUBILEE_VERSION,
        GATEWAY_INTERFACE => CGI_VER
      }

      puts "Jubilee Web Server started, Press CTRL+C to stop"
    end

    def self.start(*args, &block)
      new(*args, &block)
    end

    def load_rack_adapter(options, &block)
      if block
        app = Rack::Builder.new(&block).to_app
      else
        config = File.join(options[:chdir], 'config.ru')
        app, opts = Rack::Builder.parse_file config
      end
      #app = eval("Rack::Builder.new {( #{rackup_code}\n )}.to_app", TOPLEVEL_BINDING, config)
      Rack::Lint.new(Rack::CommonLogger.new(app, STDOUT))
    end
  end
end
