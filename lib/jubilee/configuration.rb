require 'rack'
module Jubilee
  class Configuration
    attr_accessor :app, :port
    def initialize(options, &block)
      @options = options
      @block = block
    end

    def load
      @app = load_rack_adapter(@options, &@block)
      @port = @options[:port]
    end

    def self.load(config)
      rackup_code = ::File.read(config)
      eval("Rack::Builder.new {( #{rackup_code}\n )}.to_app", TOPLEVEL_BINDING, config)
    end

    private
    def load_rack_adapter(options, &block)
      if block
        app = Rack::Builder.new(&block).to_app
      else
        if options[:rackup]
          Kernel.load(options[:rackup])
          app = Object.const_get(File.basename(options[:rackup], '.rb').capitalize.to_sym).new
        else
          Dir.chdir options[:chdir] if options[:chdir]
          app, opts = Rack::Builder.parse_file "config.ru"
        end
      end
      #app = eval("Rack::Builder.new {( #{rackup_code}\n )}.to_app", TOPLEVEL_BINDING, config)
      Rack::Lint.new(Rack::CommonLogger.new(app, STDOUT))
    end

  end
end
