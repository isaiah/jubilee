require 'java'
module Jubilee
  class Response
    include Const
    include org.jruby.jubilee.RackResponse

    def initialize(array)
      @status, @headers, @body = *array
      @status = @status.to_i
      @content_length = nil
      if @body.kind_of? Array and @body.size == 1
        @content_length = @body[0].bytesize
      end
    end

    def respond(response)
      no_body = STATUS_WITH_NO_ENTITY_BODY[@status]
      write_status(response)
      write_headers(response)
      if no_body
        response.end
      else
        if @body.respond_to?(:to_path)
          response.send_file(@body.to_path)
        else
          write_body(response)
          response.end
        end
      end
    rescue NativeException => e
      # Don't needlessly raise errors because of client abort exceptions
      raise unless e.cause.toString =~ /(clientabortexception|broken pipe)/i
    ensure
      @body.close if @body.respond_to?(:close)
    end

    private
    def write_status(response)
      response.status_code = @status
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
          response.put_header(key, value)
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
