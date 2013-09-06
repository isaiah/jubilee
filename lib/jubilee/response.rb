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

    def respond(response)
      no_body = @status < 200 || STATUS_WITH_NO_ENTITY_BODY[@status]
      write_status(response)
      write_headers(response)
      if no_body
        response.end
      else 
        if @body.respond_to?(:to_path)
          response.sendFile(@body.to_path)
        else
          write_body(response)
          response.end
        end
      end
    ensure
      @body.close if @body.respond_to?(:close)
    end

    private
    def write_status(response)
      response.setStatusCode(@status)
    end

    def write_headers(response)
      @headers.each do |key, values|
        case key
        when CONTENT_LENGTH
          @content_length = values
          next
        when TRANSFER_ENCODING
          @allow_chunked = false
          @content_length = nil
        end
        # Multiple values are joined by \n
        values.split(NEWLINE).each do |value|
          response.putHeader(key, value)
        end
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
