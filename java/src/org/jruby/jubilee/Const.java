package org.jruby.jubilee;

import org.jruby.Ruby;
import org.jruby.RubyArray;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: isaiah
 * Date: 11/26/12
 * Time: 11:45 AM
 */
public final class Const {

    public static final String JUBILEE_VERSION = "Jubilee Server 1.1.0";
    public static final String HTTP_11 = "HTTP/1.1";
    public static final String HTTP_10 = "HTTP/1.0";

    public static final String SCRIPT_NAME = "SCRIPT_NAME";
    public static final String SERVER_SOFTWARE = "SERVER_SOFTWARE";
    public static final String SERVER_PROTOCOL = "SERVER_PROTOCOL";
    public static final String GATEWAY_INTERFACE = "GATEWAY_INTERFACE";
    public static final String SERVER_NAME = "SERVER_NAME";
    public static final String SERVER_PORT = "SERVER_PORT";

    public static final String CGI_VER = "CGI/1.2";

    public static final String RACK_INPUT = "rack.input";

    public static final String REQUEST_METHOD = "REQUEST_METHOD";
    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String REQUEST_PATH = "REQUEST_PATH";
    public static final String REQUEST_URI = "REQUEST_URI";
    public static final String PATH_INFO = "PATH_INFO";
    public static final String QUERY_STRING = "QUERY_STRING";
    public static final String LOCALHOST = "localhost";
    public static final String PORT_80 = "80";

    public static final String REMOTE_ADDR = "REMOTE_ADDR";
    public static final String HTTP_HOST = "HTTP_HOST";
    public static final String HTTP_USER_AGENT = "HTTP_USER_AGENT";
    public static final String HTTP_ACCEPT = "HTTP_ACCEPT";
    public static final String HTTP_COOKIE = "HTTP_COOKIE";
    public static final String HTTP_ACCEPT_LANGUAGE = "HTTP_ACCEPT_LANGUAGE";
    public static final String HTTP_ACCEPT_ENCODING = "HTTP_ACCEPT_ENCODING";
    public static final String HTTP_CONNECTION = "HTTP_CONNECTION";
    public static final String HTTP_CONTENT_TYPE = "CONTENT_TYPE";
    public static final String HTTP_CONTENT_LENGTH = "CONTENT_LENGTH";

    public static class Rack {
        public static final String HTTP_AUTHORIZATION = "HTTP_AUTHORIZATION";
        public static final String HTTP_EXPECT = "HTTP_EXPECT";
        public static final String HTTP_IF_MATCH = "HTTP_IF_MATCH";
        public static final String HTTP_IF_MODIFIED_SINCE = "HTTP_IF_MODIFIED_SINCE";
        public static final String HTTP_IF_NONE_MATCH = "HTTP_IF_NONE_MATCH";
        public static final String HTTP_IF_RANGE = "HTTP_IF_RANGE";
        public static final String HTTP_IF_UNMODIFIED_SINCE = "HTTP_IF_UNMODIFIED_SINCE";
        public static final String HTTP_RANGE = "HTTP_RANGE";
        public static final String HTTP_PRAGMA = "HTTP_PRAGMA";
        public static final String HTTP_MAX_FORWARDS = "HTTP_MAX_FORWARDS";
        public static final String HTTP_REFERER = "HTTP_REFERER";
        public static final String HTTP_VIA = "HTTP_VIA";
        public static final String HTTP_WARNING = "HTTP_WARNING";

        public static final String HTTP_X_REQUESTED_WITH = "HTTP_X_REQUESTED_WITH"; // xhr
        public static final String HTTP_DNT = "HTTP_DNT"; // do-not-track
        public static final String HTTP_X_FORWARDED_FOR = "HTTP_X_FORWARDED_FOR"; // original ip

        public static final String HTTP_CONTENT_MD5 = "HTTP_CONTENT_MD5";
    }

    public static class Vertx {
        // all lower case since vertx has converted them
        public static final String COOKIE = "cookie";
        public static final String USER_AGENT = "user-agent";
        public static final String ACCEPT = "accept";
        public static final String ACCEPT_LANGUAGE = "accept-language";
        public static final String ACCEPT_ENCODING = "accept-encoding";
        public static final String AUTHORIZATION = "authorization";
        public static final String CONNECTION = "connection";
        public static final String CONTENT_TYPE = "content-type";
        public static final String CONTENT_LENGTH = "content-length";
        public static final String CONTENT_MD5 = "content-md5";
        public static final String HOST = "host";

        public static final String EXPECT = "expect";
        public static final String IF_MATCH = "if-match";
        public static final String IF_MODIFIED_SINCE = "if-modified-since";
        public static final String IF_NONE_MATCH = "if-none-match";
        public static final String IF_RANGE = "if-range";
        public static final String IF_UNMODIFIED_SINCE = "if-unmodified-since";
        public static final String RANGE = "range";
        public static final String PRAGMA = "pragma";
        public static final String MAX_FORWARDS = "max-forwards";
        public static final String REFERER = "referer";
        public static final String VIA = "via";
        public static final String WARNING = "warning";

        // Non-standard request headers http://en.wikipedia.org/wiki/List_of_HTTP_header_fields#Common_non-standard_request_headers
        public static final String X_REQUESTED_WITH = "x-requested-with"; // xhr
        public static final String DNT = "dnt"; // do-not-track
        public static final String X_FORWARDED_FOR = "x-forwarded-for"; // original ip
    }

    public static Map<String, String> ADDITIONAL_HEADERS = new HashMap<String, String>();

    static {
        ADDITIONAL_HEADERS.put(Vertx.EXPECT, Rack.HTTP_EXPECT);
        ADDITIONAL_HEADERS.put(Vertx.IF_MATCH, Rack.HTTP_IF_MATCH);
        ADDITIONAL_HEADERS.put(Vertx.IF_MODIFIED_SINCE, Rack.HTTP_IF_MODIFIED_SINCE);
        ADDITIONAL_HEADERS.put(Vertx.IF_NONE_MATCH, Rack.HTTP_IF_NONE_MATCH);
        ADDITIONAL_HEADERS.put(Vertx.IF_RANGE, Rack.HTTP_IF_RANGE);
        ADDITIONAL_HEADERS.put(Vertx.IF_UNMODIFIED_SINCE, Rack.HTTP_IF_UNMODIFIED_SINCE);
        ADDITIONAL_HEADERS.put(Vertx.RANGE, Rack.HTTP_RANGE);
        ADDITIONAL_HEADERS.put(Vertx.PRAGMA, Rack.HTTP_PRAGMA);
        ADDITIONAL_HEADERS.put(Vertx.MAX_FORWARDS, Rack.HTTP_MAX_FORWARDS);
        ADDITIONAL_HEADERS.put(Vertx.REFERER, Rack.HTTP_REFERER);
        ADDITIONAL_HEADERS.put(Vertx.VIA, Rack.HTTP_VIA);
        ADDITIONAL_HEADERS.put(Vertx.WARNING, Rack.HTTP_WARNING);
        ADDITIONAL_HEADERS.put(Vertx.X_REQUESTED_WITH, Rack.HTTP_X_REQUESTED_WITH);
        ADDITIONAL_HEADERS.put(Vertx.DNT, Rack.HTTP_DNT);
        ADDITIONAL_HEADERS.put(Vertx.X_FORWARDED_FOR, Rack.HTTP_X_FORWARDED_FOR);
        ADDITIONAL_HEADERS.put(Vertx.CONTENT_MD5, Rack.HTTP_CONTENT_MD5);
        ADDITIONAL_HEADERS.put(Vertx.AUTHORIZATION, Rack.HTTP_AUTHORIZATION);
    }

    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    public static final String URL_SCHEME = "rack.url_scheme";
    public static final String RACK_VERSION = "rack.version";
    public static final String RACK_ERRORS = "rack.errors";
    public static final String RACK_MULTITHREAD = "rack.multithread";
    public static final String RACK_MULTIPROCESS = "rack.multiprocess";
    public static final String RACK_RUNONCE = "rack.run_once";

    public static final String RACK_HIJACK_P = "rack.hijack?";

    private Const() {
    }

    // Internal
    public static final String END_OF_BODY = "__EOF__";
    public static final byte EOL = '\n';
}
