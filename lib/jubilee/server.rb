require 'stringio'

module Jubilee
  class Server
    include Const

    def initialize(app, &block)
      @app = app
      ENV["RACK_ENV"] ||= "development"
      puts "Jubilee Web Server started, Press CTRL+C to stop"
    end

    def self.start(*args, &block)
      new(*args, &block)
    end

    def self.load(config)
      rackup_code = ::File.read(config)
      eval("Rack::Builder.new {( #{rackup_code}\n )}.to_app", TOPLEVEL_BINDING, config)
    end

    def load_rack_adapter(options, &block)
      if block
        app = Rack::Builder.new(&block).to_app
      else
        if options[:chdir]
          Dir.chdir options[:chdir]
          app, opts = Rack::Builder.parse_file "config.ru"
        else
          Kernel.load(options[:rackup])
          app = Object.const_get(File.basename(options[:rackup], '.rb').capitalize.to_sym).new
        end
      end
      #app = eval("Rack::Builder.new {( #{rackup_code}\n )}.to_app", TOPLEVEL_BINDING, config)
      Rack::Lint.new(Rack::CommonLogger.new(app, STDOUT))
    end
  end
end
