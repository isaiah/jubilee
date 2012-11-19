module Jubilee
  class Runner
    # Parsed options
    attr_accessor :options
    def initialize(argv)
      @argv = argv
      @options = {
        chdir: Dir.pwd,
        environment: ENV['RACK_ENV'] || 'development',
        address: Server::DEFAULT_HOST,
        port: Server::DEFAULT_PORT,
      }
    end

    def run
      server = Server.new(@options)
      server.app = load_rack_adapter
    end

    def load_rack_adapter
      Rack::Adapter.load(@options[:chdir])
    end
  end
end
