module Jubilee
  module Const
    DEFAULT_HOST = 'localhost'
    DEFAULT_PORT = 3001


    JUBILEE_VERSION = VERSION = "0.1.0".freeze
    HTTP_11 = "HTTP/1.1".freeze
    HTTP_10 = "HTTP/1.0".freeze

    SERVER_SOFTWARE = "SERVER_SOFTWARE".freeze
    SERVER_PROTOCOL = "SERVER_PROTOCOL".freeze
    GATEWAY_INTERFACE = "GATEWAY_INTERFACE".freeze
    SERVER_NAME = "SERVER_NAME".freeze
    SERVER_PORT = "SERVER_PORT".freeze

    CGI_VER = "CGI/1.2".freeze

    RACK_INPUT = "rack.input".freeze

    REQUEST_METHOD = 'REQUEST_METHOD'.freeze
    GET = 'GET'.freeze
    POST = "POST".freeze
    REQUEST_PATH = "REQUEST_PATH".freeze
    REQUEST_URI = "REQUEST_URI".freeze
    PATH_INFO = "PATH_INFO".freeze
    QUERY_STRING = "QUERY_STRING".freeze

    CONTENT_LENGTH = "Content-Length".freeze
    TRANSFER_ENCODING = "Transfer-Encoding".freeze

    HTTP_VERSION = "HTTP_VERSION".freeze
    HTTP_HOST = "HTTP_HOST".freeze
    HTTP_USER_AGENT = "HTTP_USER_AGENT".freeze
    HTTP_ACCEPT = "HTTP_ACCEPT".freeze
    HTTP_COOKIE = "HTTP_COOKIE".freeze
    HTTP_ACCEPT_LANGUAGE = "HTTP_ACCEPT_LANGUAGE".freeze
    HTTP_ACCEPT_ENCODING = "HTTP_ACCEPT_ENCODING".freeze
    HTTP_CONNECTION = "HTTP_CONNECTION".freeze
  end
end
