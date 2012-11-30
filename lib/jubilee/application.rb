module Jubilee
  class Application
    def initialize(rack_app)
      unless @app = rack_app
        raise "rack application not found, make sure the rackup file path is correct"
      end
    end

    def call(env)
      Response.new(@app.call(env))
    end
  end
end
