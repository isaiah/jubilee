require 'rack/methodoverride'
module Jubilee
  class Server < VertxServer
    def initialize(app, opts = {})
      app = Rack::MethodOverride.new(app)
      options = {Port: 3215, ssl: false, number_of_workers: 4}.merge(opts)
      if (options[:ssl]) && options[:keystore_path].nil?
          raise ArgumentError, "Please provide a keystore for ssl"
      end
      super(Application.new(app), options)
    end
  end
end
