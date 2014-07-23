require 'java'
module Jubilee
  class Response
    include Const
    include org.jruby.jubilee.RackResponse

    def initialize(array)
      @status, @headers, @body = *array
      @status                  = @status.to_i
      @content_length          = nil
      @chunked                 = false
      @hijack                  = nil
      if @body.kind_of? Array and @body.size == 1
        @content_length = @body[0].bytesize
      end
    end

    # See Rack::Utils::
    def respond(response)
      no_body = @status < 200 || STATUS_WITH_NO_ENTITY_BODY[@status]
      write_status(response)
      write_headers(response)
      if @hijack
        @hijack.call(response.net_socket)
        return
      end
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
      puts e
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
          if @chunked = (values == CHUNKED)
            @content_length = nil
          end
        when HIJACK
          @hijack = values
          next
        end
        # Multiple values are joined by \n
        response.put_header(key, values)
      end
    end

    def write_body(response)
      response.put_default_headers
      if @content_length
        response.put_header(CONTENT_LENGTH, @content_length.to_s)
      else
        response.chunked = true
      end

      @body.each do |part|
        if chunk = @chunked ? self.class.strip_term_markers(part) : part
          response.write(chunk)
        end
      end
    end

    def self.strip_term_markers(chunk)
      # Heavily copied from jruby-rack's rack/response.rb
      term = "\r\n"
      tail = "0#{term}#{term}".freeze
      term_regex = /^([0-9a-fA-F]+)#{Regexp.escape(term)}(.+)#{Regexp.escape(term)}/mo
      if chunk == tail
        # end of chunking, do nothing
        nil
      elsif chunk =~ term_regex
        # format is (size.to_s(16)) term (chunk) term
        # if the size doesn't match then this is some
        # output that just happened to match our regex
        if $1.to_i(16) == $2.bytesize
          $2
        else
          chunk
        end
      else
        chunk
      end
    end
  end
end
