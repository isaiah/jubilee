require 'optparse'
require 'jubilee'
require 'java'

module Jubilee
  class CLI
    # Parsed options
    attr_accessor :options
    def initialize(argv)
      @argv = argv
      setup_options
    end

    def parse_options
      argv = @argv.dup
      @parser.parse! argv
      if @argv.last
        @options[:rackup] = argv.shift
      end
    end

    def run
      parse_options
      if @options[:daemon]
        puts "Starting Jubilee in daemon mode..."
        `jubilee_d #{(@argv - ["-d", "--daemon"]).join(" ")}`
      else
        @config = Jubilee::Configuration.new(@options)
        server = Jubilee::Server.new(@config.app, @options)
        server.start
        puts "Jubilee is listening on port #{@config.port}, press Ctrl+C to quit"
        starter = org.jruby.jubilee.deploy.Starter.new
        starter.block
      end
    end

    def setup_options
      @options = {
        debug: false,
        daemon: false,
        Port: 3215,
        ssl: false,
        environment: ENV["RACK_ENV"] || "development"
      }
      @parser = OptionParser.new do |o|
        o.separator ""
        o.separator "Server options:"

        #o.on "-c", "--config PATH", "Load PATH as a config file" do |arg|
        #  @options[:config_file] = arg
        #end
        o.on "-d", "--daemon", "Daemonize the server" do
          @options[:daemon] = true
        end
        o.on "--dir DIR", "Change to DIR before starting" do |arg|
          @options[:chdir] = arg
        end
        o.on "-p", "--port PORT", "Defind which PORT the server should bind" do |arg|
          @options[:Port] = arg
        end
        o.on "--host HOST", "Defind which HOST the server should bind, default 0.0.0.0" do |arg|
          @options[:Host] = arg
        end
        o.on "-e", "--environment ENV", "Rack environment" do |arg|
          @options[:environment] = arg
        end
        o.separator ""
        o.separator "SSL options:"
        o.on "--ssl", "Enable SSL connection" do 
          @options[:ssl] = true
        end
        o.on "--ssl-keystore PATH", "SSL keystore path" do |arg|
          @options[:keystore_path] = arg
        end
        o.on "--ssl-password PASS", "SSL keystore password" do |arg|
          @options[:keystore_password] = arg
        end
        o.separator ""
        o.separator "Event bus options"
        o.on "--eventbus PREFIX", "Event bus prefix" do |arg|
          @options[:eventbus_prefix] = arg
        end

        o.separator ""
        o.separator "Common options:"
        o.on "--verbose", "Log low level debug information" do
          @options[:debug] = true
        end
        o.on "-q", "--quiet" do
          @options[:quiet] = true
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
