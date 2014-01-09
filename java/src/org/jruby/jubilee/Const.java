package org.jruby.jubilee;

/**
 * Created with IntelliJ IDEA.
 * User: isaiah
 * Date: 11/26/12
 * Time: 11:45 AM
 */
public final class Const {

    public static final String JUBILEE_VERSION = "Jubilee(1.1.0)";
    public static final String HTTP_11 = "HTTP/1.1";
    public static final String HTTP_10 = "HTTP/1.0";

    public static final String LOCALHOST = "localhost";
    public static final String PORT_80 = "80";

    public static class Vertx {
        public static final String CONTENT_LENGTH = "Content-Length";
    }

    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    public static final String HOST = "Host";

    private Const() {
    }

    // Internal
    public static final String END_OF_BODY = "__EOF__";
    public static final byte EOL = '\n';
}
