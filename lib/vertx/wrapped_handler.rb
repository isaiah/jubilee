module Vertx

  # @private
  class ARWrappedHandler
    include org.vertx.java.core.AsyncResultHandler
  
    def initialize(handler, &result_converter)
      @handler = handler
      @result_converter = result_converter
    end
  
    def handle(future_result)
      if @handler
        if future_result.succeeded
          if @result_converter
            @handler.call(nil, @result_converter.call(future_result.result))
          else
            @handler.call(nil, future_result.result)
          end
        else
          @handler.call(future_result.cause, nil)
        end
      end
    end
  
  end
  
end