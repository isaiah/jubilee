require 'optparse'
require 'jubilee'
require 'java'

java_import java.lang.System

module Jubilee
  class CLI
    # Parsed options
    attr_accessor :options
    def initialize(argv)
      @argv = argv
      setup_options
    end

    def test_java_version!(version)
      if version[0..2] < "1.7"
        puts("Error: Jubilee requires JDK 1.7.0 or later. You can use the official Oracle distribution or the OpenJDK version.")
        exit 1
      end
    end

    def parse_options
      argv = @argv.dup
      @parser.parse! argv
      if @argv.last
        @options[:rackup] = argv.shift
      end
    end

    def run
      test_java_version!(System.getProperties["java.runtime.version"])
      parse_options

      ENV["RACK_ENV"] = @options[:environment]

      if @options[:daemon]
        puts "Starting Jubilee in daemon mode..."
        `jubilee_d #{(@argv - ["-d", "--daemon"]).join(" ")}`
      else
        @config = Jubilee::Configuration.new(@options)
        server = Jubilee::Server.new(nil, @config.options)
        #server.start
        thread = Thread.current
        Signal.trap("INT") do
          server.stop
          puts "Jubilee is shutting down gracefully..."
          thread.wakeup
        end
        puts "Jubilee is listening on port #{@config.options[:Port]}, press Ctrl+C to quit"
        sleep
      end
    end

    def setup_options
      @options = {
        debug: false,
        daemon: false,
        ssl: false,
        Port: 8080,
        environment: ENV["RACK_ENV"] || "development"
      }
      @parser = OptionParser.new do |o|
        o.separator ""
        o.separator "Server options:"

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
          @options[:Port] = arg.to_i
        end
        o.on "-b", "--host HOST", "Defind which HOST the server should bind, default 0.0.0.0" do |arg|
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
          @options[:ssl_keystore] = arg
        end
        o.on "--ssl-password PASS", "SSL keystore password" do |arg|
          @options[:ssl_keystore] = arg
        end
        o.separator ""
        o.separator "Event bus options:"
        o.on "--eventbus PREFIX", "Event bus prefix, use allow-all policy by default" do |arg|
          @options[:eventbus_prefix] = arg
        end

        o.separator ""
        o.separator "Clustering options:"
        o.on "--cluster", "Enable clustering" do
          @options[:cluster_host] = "0.0.0.0"
        end
        o.on "--cluster-port PORT", "If the cluster option has also been specified then this determines which port will be used for cluster communication with other Vert.x instances. Default is 0 -which means 'chose a free ephemeral port. You don't usually need to specify this parameter unless you really need to bind to a specific port." do |port|
          @options[:cluster_port] = port.to_i
        end
        o.on "--cluster-host HOST", "If the cluster option has also been specified then this determines which host address will be used for cluster communication with other Vert.x instances. By default it will try and pick one from the available interfaces. If you have more than one interface and you want to use a specific one, specify it here." do |host|
          @options[:cluster_host] = host
        end

        o.separator ""
        o.separator "Common options:"
        o.on "--verbose", "Log low level debug information" do
          @options[:debug] = true
        end

        o.on "-q", "--quiet", "Disable logging" do
          @options[:quiet] = true
        end

        o.on "-v", "--version", "Print the version information" do
          puts "jubilee version #{Jubilee::Version::STRING} on Vert.x 2.1M3"
          exit 0
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
