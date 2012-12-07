require 'java'
module Jubilee
  class Response
    include Const
    include org.jruby.jubilee.RackResponse

    def initialize(array)
      @status, @headers, @body = *array
      @content_length = nil
      if @body.kind_of? Array and @body.size == 1
        @content_length = @body[0].bytesize
      end
    end

    def getStatus
      @status
    end

    def getHeaders
      @headers
    end

    def getBody
      @body
    end

    def respond(response)
      if @body.respond_to?(:to_path)
        response.sendFile(@body.to_path)
      else
        write_status(response)
        write_headers(response)
        write_body(response)
        response.end
      end
    ensure
      @body.close if @body.respond_to?(:close)
    end

    private
    def write_status(response)
      response.statusCode = @status
    end

    def write_headers(response)
      @headers.each do |key, value|
        case key
        when CONTENT_LENGTH
          @content_length = value
          next
        when TRANSFER_ENCODING
          @allow_chunked = false
          @content_length = nil
        end
        response.putHeader(key, value)
      end
    end

    def write_body(response)
      if @content_length
        response.putHeader(CONTENT_LENGTH, @content_length.to_s)
      else
        response.setChunked(true)
      end

      @body.each do |part|
        response.write(part)
      end
    end
  end
end
