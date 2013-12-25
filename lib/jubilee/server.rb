require 'rack/methodoverride'
module Jubilee
  class Server < VertxServer
    def initialize(app, opts = {})
      options = {Port: 3215, ssl: false}.merge(opts)
      if (options[:ssl]) && options[:keystore_path].nil?
          raise ArgumentError, "Please provide a keystore for ssl"
      end
      super(Application.new(app), options)
    end
  end
end
