module Jubilee
  class Server < VertxServer
    def initialize(app, port = 3215)
      super(Application.new(app), port)
    end
  end
end
