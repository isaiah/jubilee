require 'optparse'

module Jubilee
  class CLI
    # Parsed options
    attr_accessor :options
    def initialize(argv)
      @argv = argv
      setup_options
    end

    def parse_options
      @parser.parse! @argv
      if @argv.last
        @options[:rackup] = @argv.shift
      end
    end

    def run
      parse_options
      @config = Jubilee::Configuration.new(@options)
      @config.load
      server = Jubilee::Server.new(@config.app, @config.port)
      server.start
      puts "Jubilee start is listening on port #{@config.port}, press Ctrl+C to quit"
      while true
        begin
        rescue Interrupt
          puts "* Bye!"
          server.stop
        end
      end
    end

    def setup_options
      @options = {
        debug: false,
        daemon: false,
        port: 3212,
        environment: "development"
      }
      @parser = OptionParser.new do |o|
        o.on "-c", "--config PATH", "Load PATH as a config file" do |arg|
          @options[:config_file] = arg
        end
        o.on "-d", "--daemon", "Daemonize the server" do
          @options[:daemon] = true
        end
        o.on "--dir DIR", "Change to DIR before starting" do |arg|
          @options[:chdir] = arg
        end
        o.on "-p", "--port PORT", "Defind which PORT the server should bind" do |arg|
          @options[:port] = arg
        end
        o.on "--verbose", "Log low level debug information" do
          @options[:debug] = true
        end
        o.on "-e", "--environment ENV", "Rack environment" do |arg|
          @options[:environment] = arg
        end
      end

      @parser.banner = "jubilee <options> <rackup file>"
      @parser.on_tail "-h", "--help", "Show this message" do
        puts @parser
        exit 1
      end
    end
  end
end
