module Jubilee
  class Server < VertxServer
    def initialize(app, opts = {})
      options = {port: 3215, ssl: false}.merge(opts)
      if (options[:ssl])
        if options[:keystore].nil?
          raise ArgumentError, "Please provide a keystore for ssl"
        else
          super(Application.new(app), options[:port], options[:ssl], options[:keystore], options[:keystore_password])
        end
      else
        super(Application.new(app), options[:port])
      end
    end
  end
end
