require 'rack/handler'
require 'jubilee'
require 'java'

module Rack
  module Handler
    module Jubilee
      DEFAULT_OPTIONS = {
        :host => '0.0.0.0',
        :port => 3000,
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

        @server = ::Jubilee::Server.new(app, options)

        puts "Jubilee starting..."
        puts "Environment: #{ENV['RACK_ENV']}"

        yield @server if block_given?

        @server.start
        @starter = org.jruby.jubilee.deploy.Starter.new
        @starter.block
      end

      def self.shutdown
        @server.stop{ @starter.unblock }
        exit
      end
    end
    register :jubilee, Jubilee
  end
end
