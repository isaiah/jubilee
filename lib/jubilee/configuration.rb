# -*- encoding: binary -*-

module Jubilee
  # Implements a simple DSL for configuring a Jubilee server
  #
  # See https://github.com/isaiah/jubilee/examples/jubilee.conf.rb
  # for example configuration files.
  class Configuration

    attr_accessor :config_file
    attr_reader :options

    def initialize(options, &block)
      @config_file = options.delete(:config_file)
      @options = options.dup
      @block = block

      reload
      # initialize vertx as early as possible
      # XXX vertx is managed by PlatformManager now
      if chost = @options[:cluster_host]
        if cport = @options[:cluster_port]
          org.jruby.jubilee.vertx.JubileeVertx.init(cport.to_java(:int), chost.to_java)
        else
          org.jruby.jubilee.vertx.JubileeVertx.init(chost.to_java)
        end
      else
        org.jruby.jubilee.vertx.JubileeVertx.init()
      end
    end

    def reload
      instance_eval(File.read(config_file), config_file) if config_file
      load_rack_adapter(&@block)
    end

    # sets the host and port jubilee listens to +address+ may be an Integer port 
    # number for a TCP port or an "IP_ADDRESS:PORT" for TCP listeners
    #
    #   listen 3000 # listen to port 3000 on all TCP interfaces
    #   listen "127.0.0.1:3000"  # listen to port 3000 on the loopback interface
    #   listen "[::1]:3000" # listen to port 3000 on the IPv6 loopback interface
    def listen(address)
      @options[:Host], @options[:Port] = expand_addr(address, :listen)
    end

    # sets the working directory for jubilee
    def working_directory(path)
      @options[:chdir] = File.expand_path(path)
    end

    # sets the RACK_ENV environment variable
    def environment(env)
      @options[:environment] = env
    end

    # set the event bus bridge prefix, prefix, options
    # eventbus /eventbus, inbound: [{foo:bar}], outbound: [{foo: bar}]
    # will set the event bus prefix as eventbus "/eventbus", it can be
    # connected via new EventBus("http://localhost:8080/eventbus"), inbound and
    # outbound options are security measures that will filter the messages
    def eventbus(prefix, options = {})
      @options[:eventbus_prefix] = prefix
      @options[:eventbus_inbound] = options[:inbound]
      @options[:eventbus_outbound] = options[:outbound]
    end

    # Set the host and port to be discovered by other jubilee instances in the network
    # +address+ may be an Integer port number for a TCP port or an
    # "IP_ADDRESS:PORT" for TCP listeners, or "IP_ADDRESS" and let the system
    # to assign a port
    #
    #    clustering true # enable cluster mode, default to "0.0.0.0:5701"
    #    clustering "0.0.0.0"
    #    clustering "0.0.0.0:5701"
    #    clustering 5701
    def clustering(address)
      if address == true
        @options[:cluster_host] = "0.0.0.0"
      else
        @options[:cluster_host], @options[:cluster_port] = expand_addr(address, :clustering)
      end
    end

    # enable debug messages
    def debug(bool)
      set_bool(:debug, bool)
    end

    # enable daemon mode
    def daemonize(bool)
      set_bool(:deamon, bool)
    end

    # enable https mode, provide the :keystore path and password
    def ssl(options = {})
      set_path(:ssl_keystore, options[:keystore])
      @options[:ssl_password] = options[:password]
      @options[:ssl] = true
    end

    # sets the path for the PID file of the jubilee event loop
    def pid(path)
      set_path(:pid, path)
    end

    # Allows redirecting $stderr to a given path, if you are daemonizing and
    # useing the default +logger+, this defautls to log/jubilee.stderr.log
    def stderr_path(path)
      set_path(:stderr_path, path)
    end

    # live stderr_path, this defaults to log/jubilee.stdout.log when daemonized
    def stdout_path(path)
      set_path(:stdout_path, path)
    end

    private
    def load_rack_adapter(&block)
      if block
        @options[:rackapp] = Rack::Builder.new(&block).to_app
      else
        @options[:rackup] = @opitons[:chdir] + "/" if @options[:chdir]
      end
    end

    def rackup
      @options[:rackup] || "config.ru"
    end

    def expand_addr(addr, var = nil)
      return ["0.0.0.0", addr] if addr.is_a?(Fixnum)
      case addr
      when %r{\A(?:\*:)?(\d+)\z}
        ["0.0.0.0", $1]
      when %r{\A\[([a-fA-F0-9:]+)\]:(\d+)\z}, %r{\A(.*):(\d+)\z}
        canonicalize_tcp($1, $2.to_i)
      when %r{\A?:\*\z}
        [addr, nil]
      else
        raise ArgumentError, "unrecognizable address #{var}=#{addr.inspect}"
      end
    end

    def set_int(var, n, min)
      Integer === n or raise ArgumentError, "not an integer: #{var}=#{n.inspect}"
      n >= min or raise ArgumentError, "too low (< #{min}): #{var}=#{n.inspect}"
      @options[var] = n
    end

    def set_path(var, path)
      case path
      when NilClass, String
        @options[var] = path
      else
        raise ArgumentError
      end
    end

    def set_bool(var, bool)
      case bool
      when true, false
        @options[var] = bool
      else
        raise ArgumentError, "#{var}=#{bool.inspect} not a boolean"
      end
    end

    def canonicalize_tcp(addr, port)
      packed = Socket.pack_sockaddr_in(port, addr)
      port, addr = Socket.unpack_sockaddr_in(packed)
      /:/ =~ addr ? ["[#{addr}]",port] : [addr, port]
    end
  end
end
