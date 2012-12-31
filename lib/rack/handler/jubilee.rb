require 'rack/handler'
require 'jubilee'
require 'java'

module Rack
  module Handler
    module Jubilee
      DEFAULT_OPTIONS = {
        :Host => '0.0.0.0',
        :Port => 3000,
        :Verbose => false
      }
      def self.run(app, options = {})
        options = DEFAULT_OPTIONS.merge(options)

        if options[:Verbose]
          app = Rack::CommonLogger.new(app, STDOUT)
        end

        if options[:environment]
          ENV["RACK_ENV"] = options[:environment].to_s
        end

        @server = ::Jubilee::Server.new(app, options)

        puts "Jubilee #{::Jubilee::Const::JUBILEE_VERSION} starting..."
        puts "* Min threads: #{min}, max threads: #{max}"
        puts "* Environment: #{ENV['RACK_ENV']}"
        puts "* Listening on tcp://#{options[:Host]}:#{options[:Port]}"

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
