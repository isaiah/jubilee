package org.jruby.jubilee.impl;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyIO;
import org.jruby.RubyHash;
import org.jruby.RubyString;
import org.jruby.jubilee.Const;
import org.jruby.jubilee.RackEnvironment;
import org.jruby.jubilee.RackInput;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServerRequest;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: isaiah
 * Date: 11/26/12
 * Time: 11:40 AM
 */
public class DefaultRackEnvironment implements RackEnvironment {
    private RubyHash env;
    private MultiMap headers;
    private Ruby runtime;

    public DefaultRackEnvironment(final Ruby runtime, final HttpServerRequest request, RackInput input, boolean isSSL, RubyArray rackVersion) {
        this.runtime = runtime;
        this.env = RubyHash.newHash(runtime);
        // DEFAULT
        env.put(Const.RACK_VERSION, rackVersion);

        env.put(Const.RACK_MULTITHREAD, runtime.getTrue());
        env.put(Const.RACK_MULTIPROCESS, runtime.getFalse());
        env.put(Const.RACK_RUNONCE, runtime.getFalse());
        if (isSSL)
            env.put(Const.URL_SCHEME, Const.HTTPS);
        else
            env.put(Const.URL_SCHEME, Const.HTTP);
        env.put(Const.SCRIPT_NAME, RubyString.newEmptyString(runtime));
        env.put(Const.RACK_HIJACK_P, runtime.getFalse());

        env.put(Const.SERVER_PROTOCOL, runtime.newString(Const.HTTP_11));
        env.put(Const.SERVER_SOFTWARE, runtime.newString(Const.JUBILEE_VERSION));
        env.put(Const.GATEWAY_INTERFACE, runtime.newString(Const.CGI_VER));

        // Parse request headers
        headers = request.headers();
        String host;
        if ((host = headers.get(Const.Vertx.HOST)) != null) {
            int colon = host.indexOf(":");
            if (colon > 0) {
                env.put(Const.SERVER_NAME, runtime.newString(host.substring(0, colon)));
                env.put(Const.SERVER_PORT, runtime.newString(host.substring(colon + 1)));
            } else {
                env.put(Const.SERVER_NAME, runtime.newString(host));
                env.put(Const.SERVER_PORT, runtime.newString(Const.PORT_80));
            }

        } else {
            env.put(Const.SERVER_NAME, runtime.newString(Const.LOCALHOST));
            env.put(Const.SERVER_PORT, runtime.newString(Const.PORT_80));
        }


        RubyIO errors = new RubyIO(runtime, runtime.getErr());
        errors.setAutoclose(false);
        env.put(Const.RACK_ERRORS, errors);
        env.put(Const.RACK_INPUT, input);
        env.put(Const.REQUEST_METHOD, runtime.newString(request.method()));
        env.put(Const.REQUEST_PATH, runtime.newString(request.path()));
        env.put(Const.REQUEST_URI, runtime.newString(request.uri()));
        env.put(Const.QUERY_STRING, orEmpty(request.query()));
        env.put(Const.REMOTE_ADDR, runtime.newString(request.remoteAddress().getHostString()));
        env.put(Const.HTTP_HOST, orEmpty(host));
        env.put(Const.HTTP_COOKIE, orEmpty(headers.get(Const.Vertx.COOKIE)));
        env.put(Const.HTTP_USER_AGENT, orEmpty(headers.get(Const.Vertx.USER_AGENT)));
        env.put(Const.HTTP_ACCEPT, orEmpty(headers.get(Const.Vertx.ACCEPT)));
        env.put(Const.HTTP_ACCEPT_LANGUAGE, orEmpty(headers.get(Const.Vertx.ACCEPT_LANGUAGE)));
        env.put(Const.HTTP_ACCEPT_ENCODING, orEmpty(headers.get(Const.Vertx.ACCEPT_ENCODING)));
        env.put(Const.HTTP_CONNECTION, orEmpty(headers.get(Const.Vertx.CONNECTION)));
        env.put(Const.HTTP_CONTENT_TYPE, orEmpty(headers.get(Const.Vertx.CONTENT_TYPE)));

        String contentLength;
        if ((contentLength = headers.get(Const.Vertx.CONTENT_LENGTH)) != null)
            env.put(Const.HTTP_CONTENT_LENGTH, contentLength);
        env.put(Const.PATH_INFO, request.path());

        // Additional headers
        for (Map.Entry<String, String> var : Const.ADDITIONAL_HEADERS.entrySet())
            setRackHeader(var.getKey(), var.getValue());
    }

    public RubyHash getEnv() {
        return env;
    }

    private RubyString orEmpty(String jString) {
        return RubyString.newString(runtime, "" + jString);
    }

    private void setRackHeader(String vertxHeader, String rackHeader) {
        if (headers.contains(vertxHeader)) env.put(rackHeader, runtime.newString(headers.get(vertxHeader)));
    }
}
