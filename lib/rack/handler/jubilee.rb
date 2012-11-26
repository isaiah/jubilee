require 'rack/handler'
require 'jubilee'

module Rack
  module Handler
    module Jubilee
      DEFAULT_OPTIONS = {
        :host => '0.0.0.0',
        :port => 8080,
        :verbose => false
      }
      def self.run(app, options = {})
        options = DEFAULT_OPTIONS.merge(options)

        if options[:verbose]
          app = Rack::CommonLogger.new(app, STDOUT)
        end

        if options[:environment]
          ENV["RACK_ENV"] = options[:environment].to_s
        end

        server = ::Jubilee::Server.new(app)

        puts "Jubilee starting..."
        puts "Environment: #{ENV['RACK_ENV']}"

        yield server if block_given?

        begin
          server.start
        rescue Interrupt
          puts "* Gracefully stopping, waiting requests to finish"
          server.stop
          puts "* Goodbye!"
        end
      end
    end
    register :jubilee, Jubilee
  end
end
