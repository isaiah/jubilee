module Jubilee
  class Server < VertxServer
    def initialize(app, opts = {})
      options = {Host: "0.0.0.0", Port: 8080, ssl: false}.merge(opts)
      if (options[:ssl]) && options[:ssl_keystore].nil?
          raise ArgumentError, "Please provide a keystore for ssl"
      end
      # Rackup passes a string value
      options[:Port] = options[:Port].to_i
      super(Application.new(app), options)
    end
  end
end
