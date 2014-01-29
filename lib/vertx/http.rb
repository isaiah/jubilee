# Copyright 2011 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

require 'vertx/streams'
require 'vertx/ssl_support'
require 'vertx/tcp_support'
require 'vertx/wrapped_handler'

module Vertx
  # An HTTP client.
  # A client maintains a pool of connections to a specific host, at a specific port. The HTTP connections can act
  # as pipelines for HTTP requests.
  # It is used as a factory for {HttpClientRequest} instances which encapsulate the actual HTTP requests. It is also
  # used as a factory for {WebSocket WebSockets}.
  #
  # @author {http://tfox.org Tim Fox}
  class HttpClient

    include SSLSupport, ClientSSLSupport, TCPSupport

    # Create a new HttpClient
    def initialize(config = {})
      @j_del = org.jruby.jubilee.vertx.JubileeVertx.vertx().createHttpClient()
      self.compression = config[:try_compression] if config.has_key?(:try_compression)
    end

    # Enables HTTP compression.
    # exposes a = method to set or unset compression support.
    # @param [Boolean] supported whether enable compression or not
    def compression=(supported)
      @j_del.setTryUseCompression(supported)
    end

    # Tests if client is trying to use HTTP compression or not
    # @return [Boolean] true if Client is trying using HTTP compression, false if it doesn't
    def compression
      @j_del.getTryUseCompression
    end

    # Letting the developer choose between compression? or compression while developing
    alias_method :compression?, :compression

    # Set the exception handler.
    # @param [Block] hndlr A block to be used as the handler
    def exception_handler(&hndlr)
      @j_del.exceptionHandler(hndlr)
      self
    end

    # Set the maximum pool size.
    # The client will maintain up to this number of HTTP connections in an internal pool
    # @param [FixNum] val. The maximum number of connections (default to 1).
    def max_pool_size=(val)
      @j_del.setMaxPoolSize(val)
      self
    end

    # Get or set the max pool size
    def max_pool_size(val = nil)
      if val
        @j_del.setMaxPoolSize(val)
        self
      else
        @j_del.getMaxPoolSize
      end
    end

    # If val is true then, after the request has ended the connection will be returned to the pool
    # where it can be used by another request. In this manner, many HTTP requests can be pipe-lined over an HTTP connection.
    # Keep alive connections will not be closed until the {#close} method is invoked.
    # If val is false then a new connection will be created for each request and it won't ever go in the pool,
    # the connection will closed after the response has been received. Even with no keep alive, the client will not allow more
    # than {#max_pool_size} connections to be created at any one time.
    # @param [Boolean] val. The value to use for keep_alive
    def keep_alive=(val)
      @j_del.setTCPKeepAlive(val)
      self
    end

    # Get or set keep alive
    def keep_alive(val = nil)
      if val
        @j_del.setKeepAlive(val)
        self
      else
        @j_del.isKeepAlive
      end
    end

    # Set the port that the client will attempt to connect to on the server on. The default value is 80
    # @param [FixNum] val. The port value.
    def port=(val)
      @j_del.setPort(val)
      self
    end

    # Get or set port
    def port(val = nil)
      if val
        @j_del.setPort(val)
        self
      else
        @j_del.getPort
      end
    end

    # Set the host name or ip address that the client will attempt to connect to on the server on.
    # @param [String] host. The host name or ip address to connect to.
    def host=(val)
      @j_del.setHost(val)
      self
    end

    # Get or set host
    def host(val = nil)
      if val
        @j_del.setHost(val)
        self
      else
        @j_del.getHost
      end
    end

    # Get or set verify host
    def verify_host(val = nil)
      if val
        @j_del.setVerifyHost(val)
        self
      else
        @j_del.isVerifyHost
      end
    end

    # Gets the max WebSocket Frame size in bytes
    # @return [int] Max WebSocket frame size
    def max_websocket_frame_size
      @j_del.getMaxWebSocketFrameSize
    end

    # Sets the maximum WebSocket frame size in bytes. Default is 65536 bytes.
    # @param [int] size
    def max_websocket_frame_size=(size)
      @j_del.setMaxWebSocketFrameSize(size)
    end

    # Attempt to connect a WebSocket to the specified URI.
    # The connect is done asynchronously and the handler is called with a  {WebSocket} on success.
    # @param [String] uri. A relative URI where to connect the WebSocket on the host, e.g. /some/path
    # @param [Block] hndlr. The handler to be called with the {WebSocket}
    def connect_web_socket(uri, &hndlr)
      @j_del.connectWebsocket(uri) { |j_ws| hndlr.call(WebSocket.new(j_ws)) }
      self
    end

    # This is a quick version of the {#get} method where you do not want to do anything with the request
    # before sending.
    # Normally with any of the HTTP methods you create the request then when you are ready to send it you call
    # {HttpClientRequest#end} on it. With this method the request is immediately sent.
    # When an HTTP response is received from the server the handler is called passing in the response.
    # @param [String] uri. A relative URI where to perform the GET on the server.
    # @param [Hash] headers. A Hash of headers to pass with the request.
    # @param [Block] hndlr. The handler to be called with the {HttpClientResponse}
    def get_now(uri, headers = nil, &hndlr)
      @j_del.getNow(uri, headers, resp_handler(hndlr))
      self
    end

    # This method returns an {HttpClientRequest} instance which represents an HTTP OPTIONS request with the specified uri.
    # When an HTTP response is received from the server the handler is called passing in the response.
    # @param [String] uri. A relative URI where to perform the OPTIONS on the server.
    # @param [Block] hndlr. The handler to be called with the {HttpClientResponse}
    def options(uri, &hndlr)
      HttpClientRequest.new(@j_del.options(uri, resp_handler(hndlr)))
    end

    # This method returns an {HttpClientRequest} instance which represents an HTTP GET request with the specified uri.
    # When an HTTP response is received from the server the handler is called passing in the response.
    # @param [String] uri. A relative URI where to perform the GET on the server.
    # @param [Block] hndlr. The handler to be called with the {HttpClientResponse}
    def get(uri, &hndlr)
      HttpClientRequest.new(@j_del.get(uri, resp_handler(hndlr)))
    end

    # This method returns an {HttpClientRequest} instance which represents an HTTP HEAD request with the specified uri.
    # When an HTTP response is received from the server the handler is called passing in the response.
    # @param [String] uri. A relative URI where to perform the HEAD on the server.
    # @param [Block] hndlr. The handler to be called with the {HttpClientResponse}
    def head(uri, &hndlr)
      HttpClientRequest.new(@j_del.head(uri, resp_handler(hndlr)))
    end

    # This method returns an {HttpClientRequest} instance which represents an HTTP POST request with the specified uri.
    # When an HTTP response is received from the server the handler is called passing in the response.
    # @param [String] uri. A relative URI where to perform the POST on the server.
    # @param [Block] hndlr. The handler to be called with the {HttpClientResponse}
    def post(uri, &hndlr)
      HttpClientRequest.new(@j_del.post(uri, resp_handler(hndlr)))
    end

    # This method returns an {HttpClientRequest} instance which represents an HTTP PUT request with the specified uri.
    # When an HTTP response is received from the server the handler is called passing in the response.
    # @param [String] uri. A relative URI where to perform the PUT on the server.
    # @param [Block] hndlr. The handler to be called with the {HttpClientResponse}
    def put(uri, &hndlr)
      HttpClientRequest.new(@j_del.put(uri, resp_handler(hndlr)))
    end

    # This method returns an {HttpClientRequest} instance which represents an HTTP DELETE request with the specified uri.
    # When an HTTP response is received from the server the handler is called passing in the response.
    # @param [String] uri. A relative URI where to perform the DELETE on the server.
    # @param [Block] hndlr. The handler to be called with the {HttpClientResponse}
    def delete(uri, &hndlr)
      HttpClientRequest.new(@j_del.delete(uri, resp_handler(hndlr)))
    end

    # This method returns an {HttpClientRequest} instance which represents an HTTP TRACE request with the specified uri.
    # When an HTTP response is received from the server the handler is called passing in the response.
    # @param [String] uri. A relative URI where to perform the TRACE on the server.
    # @param [Block] hndlr. The handler to be called with the {HttpClientResponse}
    def trace(uri, &hndlr)
      HttpClientRequest.new(@j_del.trace(uri, resp_handler(hndlr)))
    end

    # This method returns an {HttpClientRequest} instance which represents an HTTP CONNECT request with the specified uri.
    # When an HTTP response is received from the server the handler is called passing in the response.
    # @param [String] uri. A relative URI where to perform the CONNECT on the server.
    # @param [Block] hndlr. The handler to be called with the {HttpClientResponse}
    def connect(uri, &hndlr)
      HttpClientRequest.new(@j_del.connect(uri, resp_handler(hndlr)))
    end

    # This method returns an {HttpClientRequest} instance which represents an HTTP PATCH request with the specified uri.
    # When an HTTP response is received from the server the handler is called passing in the response.
    # @param [String] uri. A relative URI where to perform the PATCH on the server.
    # @param [Block] hndlr. The handler to be called with the {HttpClientResponse}
    def patch(uri, &hndlr)
      HttpClientRequest.new(@j_del.patch(uri, resp_handler(hndlr)))
    end

    # This method returns an {HttpClientRequest} instance which represents an HTTP request with the specified method and uri.
    # When an HTTP response is received from the server the handler is called passing in the response.
    # @param [String] method. The HTTP method. Can be one of OPTIONS, HEAD, GET, POST, PUT, DELETE, TRACE, CONNECT.
    # @param [String] uri. A relative URI where to perform the OPTIONS on the server.
    # @param [Block] hndlr. The handler to be called with the {HttpClientResponse}
    def request(method, uri, &hndlr)
      HttpClientRequest.new(@j_del.request(method, uri, resp_handler(hndlr)))
    end

    # Close the client. Any unclosed connections will be closed.
    def close
      @j_del.close
    end

    # @private
    def resp_handler(hndlr)
      Proc.new { |j_del| hndlr.call(HttpClientResponse.new(j_del)) }
    end

    private :resp_handler

  end

  # Encapsulates a client-side HTTP request.
  #
  # Instances of this class are created by an {HttpClient} instance, via one of the methods corresponding to the
  # specific HTTP methods, or the generic {HttpClient#request} method.
  #
  # Once an instance of this class has been obtained, headers can be set on it, and data can be written to its body,
  # if required. Once you are ready to send the request, the {#end} method must called.
  #
  # Nothing is sent until the request has been internally assigned an HTTP connection. The {HttpClient} instance
  # will return an instance of this class immediately, even if there are no HTTP connections available in the pool. Any requests
  # sent before a connection is assigned will be queued internally and actually sent when an HTTP connection becomes
  # available from the pool.
  #
  # The headers of the request are actually sent either when the {#end} method is called, or, when the first
  # part of the body is written, whichever occurs first.
  #
  # This class supports both chunked and non-chunked HTTP.
  #
  # @author {http://tfox.org Tim Fox}
  class HttpClientRequest

    include WriteStream

    # @private
    def initialize(j_del)
      @j_del = j_del
    end

    # MultiMap of headers for the request
    def headers
      if !@headers
        @headers = MultiMap.new(@j_del.headers)
      end
      @headers
    end

    # Inserts a header into the request.
    # @param [String] key The header key
    # @param [Object] value The header value. to_s will be called on the value to determine the actual String value to insert.
    # @return [HttpClientRequest] self So multiple operations can be chained.
    def put_header(key, value)
      @j_del.putHeader(key, value.to_s)
      self
    end

    # Write a [String] to the request body.
    # @param [String] str. The string to write.
    # @param [String] enc. The encoding to use.
    # @return [HttpClientRequest] self So multiple operations can be chained.
    def write_str(str, enc = "UTF-8")
      @j_del.write(str, enc)
      self
    end

    # Forces the head of the request to be written before {#end} is called on the request. This is normally used
    # to implement HTTP 100-continue handling, see {#continue_handler} for more information.
    # @return [HttpClientRequest] self So multiple operations can be chained.
    def send_head
      @j_del.sendHead
      self
    end

    # Ends the request. If no data has been written to the request body, and {#send_head} has not been called then
    # the actual request won't get written until this method gets called.
    # Once the request has ended, it cannot be used any more, and if keep alive is true the underlying connection will
    # be returned to the {HttpClient} pool so it can be assigned to another request.
    def end
      @j_del.end
      self
    end

    # Same as {#write_buffer_and_end} but writes a String
    # @param [String] str The String to write
    # @param [String] enc The encoding
    def write_str_and_end(str, enc = "UTF-8")
      @j_del.end(str, enc)
      self
    end

    # Same as {#end} but writes some data to the response body before ending. If the response is not chunked and
    # no other data has been written then the Content-Length header will be automatically set
    # @param [Buffer] chunk The Buffer to write
    def write_buffer_and_end(chunk)
      @j_del.end(chunk._to_java_buffer)
      self
    end

    # Sets whether the request should used HTTP chunked encoding or not.
    # @param [Boolean] val. If val is true, this request will use HTTP chunked encoding, and each call to write to the body
    # will correspond to a new HTTP chunk sent on the wire. If chunked encoding is used the HTTP header
    # 'Transfer-Encoding' with a value of 'Chunked' will be automatically inserted in the request.
    # If chunked is false, this request will not use HTTP chunked encoding, and therefore if any data is written the
    # body of the request, the total size of that data must be set in the 'Content-Length' header before any
    # data is written to the request body.
    # @return [HttpClientRequest] self So multiple operations can be chained.
    def chunked=(val)
      @j_del.setChunked(val)
      self
    end

    # Get or set chunked
    def chunked(val = nil)
      if val
        @j_del.setChunked(val)
        self
      else
        @j_del.getChunked
      end
    end

    # If you send an HTTP request with the header 'Expect' set to the value '100-continue'
    # and the server responds with an interim HTTP response with a status code of '100' and a continue handler
    # has been set using this method, then the handler will be called.
    # You can then continue to write data to the request body and later end it. This is normally used in conjunction with
    # the {#send_head} method to force the request header to be written before the request has ended.
    # @param [Block] hndlr. The handler
    def continue_handler(&hndlr)
      @j_del.continueHandler(hndlr)
      self
    end

    # Get or set timeout
    def timeout(val = nil)
      if val
        @j_del.setTimeout(val)
        self
      else
        @j_del.getTimeout
      end
    end

  end

  # Encapsulates a client-side HTTP response.
  #
  # An instance of this class is provided to the user via a handler that was specified when one of the
  # HTTP method operations, or the generic {HttpClient#request} method was called on an instance of {HttpClient}.
  #
  # @author {http://tfox.org Tim Fox}
  class HttpClientResponse

    include ReadStream

    # @private
    def initialize(j_del)
      @j_del = j_del
    end

    # @return [FixNum] the HTTP status code of the response.
    def status_code
      @j_del.statusCode
    end

    # @return [String] the status message
    def status_message
      @j_del.statusMessage
    end

    # Get a header value
    # @param [String] key. The key of the header.
    # @return [String] the header value.
    def header(key)
      @j_del.getHeader(key)
    end

    # Get all the headers in the response.
    # @return [MultiMap]. The headers
    def headers
      if !@headers
        @headers = MultiMap.new(@j_del.headers)
      end
      @headers
    end

    # Get all the trailers in the response.
    # @return [MultiMap]. The trailers
    def trailers
      if !@trailers
        @trailers = MultiMap.new(@j_del.trailers)
      end
      @trailers
    end

    # Get all cookies
    def cookies
      if !@cookies
        @cookies = @j_del.cookies
      end
      @cookies
    end

    # Set a handler to receive the entire body in one go - do not use this for large bodies
    def body_handler(&hndlr)
      @j_del.bodyHandler(hndlr)
      self
    end

  end

  # Represents a WebSocket.
  #
  # Instances of this class are created by an {HttpClient} instance when a client succeeds in a WebSocket handshake with a server.
  # Once an instance has been obtained it can be used to send or receive buffers of data from the connection,
  # a bit like a TCP socket.
  #
  # @author {http://tfox.org Tim Fox}
  class WebSocket

    include ReadStream, WriteStream

    # @private
    def initialize(j_ws)
      @j_del = j_ws
      @binary_handler_id = EventBus.register_simple_handler { |msg|
        write_binary_frame(msg.body)
      }
      @text_handler_id = EventBus.register_simple_handler { |msg|
        write_text_frame(msg.body)
      }
      @j_del.closeHandler(Proc.new {
        EventBus.unregister_handler(@binary_handler_id)
        EventBus.unregister_handler(@text_handler_id)
        @close_handler.call if @close_handler
      })
    end

    # Write data to the WebSocket as a binary frame
    # @param [Buffer] buffer. Data to write.
    def write_binary_frame(buffer)
      @j_del.writeBinaryFrame(buffer._to_java_buffer)
      self
    end

    # Write data to the WebSocket as a text frame
    # @param [String] str. String to write.
    def write_text_frame(str)
      @j_del.writeTextFrame(str)
      self
    end

    # Close the WebSocket
    def close
      @j_del.close
    end

    # When a Websocket is created it automatically registers an event handler with the system, the ID of that
    # handler is given by {#binary_handler_id}.
    # Given this ID, a different event loop can send a binary frame to that event handler using the event bus. This
    # allows you to write data to other WebSockets which are owned by different event loops.
    def binary_handler_id
      @binary_handler_id
    end

    # When a Websocket is created it automatically registers an event handler with the system, the ID of that
    # handler is given by {#text_handler_id}.
    # Given this ID, a different event loop can send a text frame to that event handler using the event bus. This
    # allows you to write data to other WebSockets which are owned by different event loops.
    def text_handler_id
      @text_handler_id
    end

    # Set a closed handler on the WebSocket.
    # @param [Block] hndlr A block to be used as the handler
    def close_handler(&hndlr)
      @close_handler = hndlr;
    end

  end

  # A map which can hold multiple values for one name / key
  #
  # @author Norman Maurer
  class MultiMap

    # @private
    def initialize(j_map)
      @j_map = j_map
    end

    # Call the handler for each header name and its value. If there are multiple values are stored for
    # a header name it will be called for each of them
    #
    def each(&hndlr)
      names.each do |name|
        values = @j_map.getAll(name)
        for v in values
          hndlr.call(name, v)
        end
      end
    end

    # Returns the value of with the specified name.  If there are
    # more than one values for the specified name, the first value is returned.
    #
    # @param name The name of the header to search
    # @return The first header value or nil if there is no such entry
    def get(name)
      @j_map.get(name)
    end

    # Returns the value of with the specified name.  If there are
    # more than one values for the specified name, the first value is returned.
    #
    # @param name The name of the header to search
    # @return The first header value or nil if there is no such entry
    def [](name)
      @j_map.get(name)
    end


    # Set a value with the specified name and value.
    #
    # @param name The name
    # @param value The value being added
    # @return self
    def []=(name, value)
      @j_map.set(name, value)
      self
    end

    # Returns the values with the specified name
    #
    # @param name The name to search
    # @return [Array] A immutable array of values which will be empty if no values
    #         are found
    def get_all(name)
      @j_map.getAll(name).to_a
    end

    # Returns true if an entry with the given name was found
    def contains(name)
      @j_map.contains(name)
    end

    # Returns true if the map is empty
    def empty?
      @j_map.isEmpty()
    end

    # Return a Set which holds all names of the entries
    #
    # @return [Set] The set which holds all names or an empty set if it is empty
    def names
     @j_map.names()
    end


    # Adds a new value with the specified name and value.
    #
    # @param name The name
    # @param value The value being added
    # @return self
    def add(name, value)
      @j_map.add(name, value)
      self
    end

    # Set a value with the specified name and value.
    #
    # @param name The name
    # @param value The value being added
    # @return self
    def set(name, value)
      @j_map.set(name, value)
      self
    end

    # Set a value with the specified name and value.
    #
    # @param name The name
    # @param value The value being added
    # @return self
    def set_all(map)
      @j_map.set(map._j_map)
      self
    end

    # Remove the values with the given name
    #
    # @param name The name
    # @return self
    def remove(name)
      @j_map.remove(name)
      self
    end

    # Remove all entries
    #
    # @return self
    def clear
      @j_map.clear()
      self
    end

    # Return the number of names in this instance
    def size
      @j_map.size()
    end

    def _j_map
      @j_map
    end
  end
end
