# Rails use this to chunk it's streaming response body, which we don't need
module Rack
  class Chunked
    class Body
      def initialize(body)
        @body = body
      end

      def each
        @body.each {|chunk| yield chunk}
      end

      def close
        @body.close if @body.respond_to?(:close)
      end
    end

    include Rack::Utils

    def initialize(app)
      @app = app
    end

    def call(env)
      status, headers, body = @app.call(env)
      headers = HeaderHash.new(headers)

      unless env['HTTP_VERSION'] == 'HTTP/1.0' ||
           STATUS_WITH_NO_ENTITY_BODY.include?(status) ||
           headers['Content-Length'] ||
           headers['Transfer-Encoding']
        headers.delete('Content-Length')
        headers.delete('Transfer-Encoding')
      end
      [status, headers, body]
    end
  end
end
