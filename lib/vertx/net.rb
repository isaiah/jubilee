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

require 'core/streams'
require 'core/ssl_support'
require 'core/tcp_support'
require 'core/wrapped_handler'
require 'socket'


module Vertx

  # Represents a TCP or SSL Server
  #
  # When connections are accepted by the server
  # they are supplied to the user in the form of a {NetSocket} instance that is passed via the handler
  # set using {#connect_handler}.
  #
  # @author {http://tfox.org Tim Fox}
  class NetServer

    include SSLSupport, ServerSSLSupport, TCPSupport, ServerTCPSupport

    # Create a new NetServer
    def initialize
      @j_del = org.vertx.java.platform.impl.JRubyVerticleFactory.vertx.createNetServer
    end

    # Supply a connect handler for this server. The server can only have at most one connect handler at any one time.
    # As the server accepts TCP or SSL connections it creates an instance of {NetSocket} and passes it to the
    # connect handler.
    # @param [Block] hndlr A block to be used as the handler
    # @return [NetServer] A reference to self so invocations can be chained
    def connect_handler(&hndlr)
      @j_del.connectHandler{ |j_socket| hndlr.call(NetSocket.new(j_socket)) }
      self
    end

    # Instruct the server to listen for incoming connections.
    # @param [FixNum] port. The port to listen on.
    # @param [FixNum] host. The host name or ip address to listen on.
    # @param [Block] hndlr. The handler will be called when the server is listening or a failure occurred.
    def listen(port, host = "0.0.0.0", &hndlr)
      @j_del.listen(port, host, ARWrappedHandler.new(hndlr) {|j_del| self})
      self
    end

    # Close the server. The handler will be called when the close is complete.
    def close(&hndlr)
      @j_del.close(ARWrappedHandler.new(hndlr))
    end

    # Get the port
    def port
      @j_del.port
    end

    # Get the host
    def host
      @j_del.host
    end

  end

  # NetClient is an asynchronous factory for TCP or SSL connections.
  #
  # Multiple connections to different servers can be made using the same instance.
  #
  # @author {http://tfox.org Tim Fox}
  class NetClient

    include SSLSupport, ClientSSLSupport, TCPSupport

    # Create a new NetClient
    def initialize
      @j_del = org.vertx.java.platform.impl.JRubyVerticleFactory.vertx.createNetClient
    end

    # Attempt to open a connection to a server. The connection is opened asynchronously and the result returned in the
    # handler.
    # @param [FixNum] port. The port to connect to.
    # @param [String] host. The host or ip address to connect to.
    # @param [Block] hndlr A block to be used as the handler. The handler will be called with an exception or the
    # {NetSocket}
    # @return [NetClient] A reference to self so invocations can be chained
    def connect(port, host = "localhost", &hndlr)
      hndlr = ARWrappedHandler.new(hndlr) { |j_socket| NetSocket.new(j_socket) }
      @j_del.connect(port, host, hndlr)
      self
    end

    # Set the reconnect attempts
    def reconnect_attempts=(val)
      @j_del.setReconnectAttempts(val)
      self
    end

    # Set or Get the reconnect attempts for a fluent API
    def reconnect_attempts(val = nil)
      if val
        @j_del.setReconnectAttempts(val)
        self
      else
        @j_del.getReconnectAttempts
      end
    end

    # Set the reconnect interval
    def reconnect_interval=(val)
      @j_del.setReconnectInterval(val)
      self
    end

    # Set or Get the reconnect interval for a fluent API
    def reconnect_interval(val = nil)
      if val
        @j_del.setReconnectInterval(val)
        self
      else
        @j_del.getReconnectInterval
      end
    end

    # Set the connect timeout
    def connect_timeout=(val)
      @j_del.setConnectTimeout(val)
      self
    end

    # Set or Get the connect timeout for a fluent API
    def connect_timeout(val = nil)
      if val
        @j_del.setConnectTimeout(val)
        self
      else
        @j_del.getConnectTimeout
      end
    end

    # Close the NetClient. Any open connections will be closed.
    def close
      @j_del.close
    end

  end


  # NetSocket is a socket-like abstraction used for reading from or writing
  # to TCP connections.
  #
  # @author {http://tfox.org Tim Fox}
  class NetSocket

    include ReadStream, WriteStream

    # @private
    def initialize(j_socket)
      @j_del = j_socket
      @local_addr = nil
      @remote_addr = nil
      @write_handler_id = EventBus.register_simple_handler { |msg|
        write(msg.body)
      }
      @j_del.closeHandler(Proc.new {
        EventBus.unregister_handler(@write_handler_id)
        @close_handler.call if @close_handler
      })
    end

    # Write a String to the socket. The handler will be called when the string has actually been written to the wire.
    # @param [String] str. The string to write.
    # @param [String] enc. The encoding to use.
    def write_str(str, enc = "UTF-8")
      @j_del.write(str, enc)
      self
    end

    # Set a closed handler on the socket.
    # @param [Block] hndlr A block to be used as the handler
    def close_handler(&hndlr)
      @close_handler = hndlr
      self
    end

    #  Tell the kernel to stream a file directly from disk to the outgoing connection, bypassing userspace altogether
    # (where supported by the underlying operating system. This is a very efficient way to stream files.
    # @param [String] file_path. Path to file to send.
    def send_file(file_path)
      @j_del.sendFile(file_path)
      self
    end

    # Close the socket
    def close
      @j_del.close
    end

    # When a NetSocket is created it automatically registers an event handler with the system. The address of that
    # handler is given by {#write_handler_id}.
    # Given this ID, a different event loop can send a buffer to that event handler using the event bus. This
    # allows you to write data to other connections which are owned by different event loops.
    def write_handler_id
      @write_handler_id
    end

    # Return the Addrinfo to which the remote end of the socket is bound
    def remote_address
      if !@remote_addr
        @remote_addr = Addrinfo.tcp(@j_del.remoteAddress().getAddress().getHostAddress(), @j_del.remoteAddress().getPort())
      end
      @remote_addr
    end


    # Return the Addrinfo to which the local end of the socket is bound
    def local_address
      if !@local_addr
        @local_addr = Addrinfo.tcp(@j_del.localAddress().getAddress().getHostAddress(), @j_del.localAddress().getPort())
      end
      @local_addr
    end
  end
end

