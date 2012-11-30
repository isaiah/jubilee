module Jubilee
  class Server < VertxServer
    def initialize(app, port = 3212)
      super(Application.new(app), port)
    end
  end
end
