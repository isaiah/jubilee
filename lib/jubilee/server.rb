module Jubilee
  class Server < PlatformManager
    def initialize(opts = {})
      options = {Host: "0.0.0.0", Port: 8080, ssl: false, instances: 1, environment: "development", quiet: true}.merge(opts)
      if (options[:ssl]) && options[:ssl_keystore].nil?
          raise ArgumentError, "Please provide a keystore for ssl"
      end
      # Rackup passes a string value
      options[:Port] = options[:Port].to_i
      super(options)
    end

    def start
    end
  end
end
