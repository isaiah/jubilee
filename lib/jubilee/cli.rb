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

      ENV["RACK_ENV"] = @options[:environment]

      if @options[:daemon]
        puts "Starting Jubilee in daemon mode..."
        `jubilee_d #{(@argv - ["-d", "--daemon"]).join(" ")}`
      else
        @config = Jubilee::Configuration.new(@options)
        server = Jubilee::Server.new(@config, @config.options)
        server.start
        puts "Jubilee is listening on port #{@config.options[:Port]}, press Ctrl+C to quit"
        starter = org.jruby.jubilee.deploy.Starter.new
        starter.block
      end
    end

    def setup_options
      @options = {
        debug: false,
        daemon: false,
        Port: 3215,
        Host: "0.0.0.0",
        ssl: false,
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
          @options[:working_directory] = arg
        end
        o.on "-p", "--port PORT", "Defind which PORT the server should bind" do |arg|
          @options[:Port] = arg.to_i
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
        o.separator "Event bus options:"
        o.on "--eventbus PREFIX", "Event bus prefix, use allow-all policy by default" do |arg|
          @options[:eventbus_prefix] = arg
        end

        o.separator ""
        o.separator "Clustering options:"
        o.on "--cluster", "Enable clustering" do
          @options[:cluster_host] = "0.0.0.0"
        end
        o.on "--cluster-port", "If the cluster option has also been specified then this determines which port will be used for cluster communication with other Vert.x instances. Default is 0 -which means 'chose a free ephemeral port. You don't usually need to specify this parameter unless you really need to bind to a specific port." do |port|
          @options[:cluster_host] = port
        end
        o.on "--cluster-host", "If the cluster option has also been specified then this determines which host address will be used for cluster communication with other Vert.x instances. By default it will try and pick one from the available interfaces. If you have more than one interface and you want to use a specific one, specify it here." do |host|
          @options[:cluster_host] = host
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
