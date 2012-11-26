module Jubilee
  class Configuration
    include Const
    attr_accessor :app
    def initialize(options, &block)
      @options = options
      load_rack_adapter(@options, &block)
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
      @app = Rack::Lint.new(Rack::CommonLogger.new(app, STDOUT))
    end

  end
end
