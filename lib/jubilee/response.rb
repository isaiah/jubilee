require 'java'
module Jubilee
  class Response
    include org.jruby.jubilee.RackResponse

    def initialize(array)
      @status, @headers, @body = *array
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
      write_status(response)
      write_headers(response)
      write_body(response)
      response.end
    end

    private
    def write_status(response)
      response.statusCode = @status
    end

    def write_headers(response)
      @headers.each do |key, value|
        response.putHeader(key, value)
      end
    end

    def write_body(response)
      enum = @body.to_enum
      begin
        ret = enum.next
        begin
          enum.peek # works as has_next?
          response.setChunked(true)
          enum.each {|part| response.write part }
        rescue StopIteration
          response.putHeader("content-length", ret.length)
          response.write ret
        end
      rescue StopIteration # empty body
        response.putHeader("content-length", "0")
        response.write ""
      end
    end
  end
end
