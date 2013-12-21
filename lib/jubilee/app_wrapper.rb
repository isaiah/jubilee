module Jubilee
  # ^:nodoc
  class AppWrapper
    def initialize(app = nil, &block)
      @app = app
      @block = block
    end

    def app
      @app ? app : @block.call
    end
  end
end
