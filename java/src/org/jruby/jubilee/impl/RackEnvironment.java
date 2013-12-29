package org.jruby.jubilee.impl;

import io.netty.handler.codec.http.HttpHeaders;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyFixnum;
import org.jruby.RubyHash;
import org.jruby.RubyIO;
import org.jruby.RubyString;

import org.jruby.jubilee.Const;
import org.jruby.jubilee.RackInput;
import org.jruby.jubilee.utils.RubyHelper;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServerRequest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class RackEnvironment {

    // When adding a key to the enum be sure to add its RubyString equivalent
    // to populateRackKeyMap below
    static enum RACK_KEY {
        RACK_INPUT, RACK_ERRORS, REQUEST_METHOD, SCRIPT_NAME,
        PATH_INFO, QUERY_STRING, SERVER_NAME, SERVER_PORT,
        CONTENT_TYPE, REQUEST_URI, REMOTE_ADDR, URL_SCHEME,
        VERSION, MULTITHREAD, MULTIPROCESS, RUN_ONCE, CONTENT_LENGTH,
        HTTPS
    }
    static final int NUM_RACK_KEYS = RACK_KEY.values().length;

    public RackEnvironment(final Ruby runtime) throws IOException {
        this.runtime = runtime;
        rackVersion = RubyArray.newArray(runtime, RubyFixnum.one(runtime), RubyFixnum.four(runtime));
        errors = new RubyIO(runtime, runtime.getErr());
        errors.setAutoclose(false);

        populateRackKeyMap();
    }

    private void populateRackKeyMap() {
        putRack("rack.input", RACK_KEY.RACK_INPUT);
        putRack("rack.errors", RACK_KEY.RACK_ERRORS);
        putRack("REQUEST_METHOD", RACK_KEY.REQUEST_METHOD);
        putRack("SCRIPT_NAME", RACK_KEY.SCRIPT_NAME);
        putRack("PATH_INFO", RACK_KEY.PATH_INFO);
        putRack("QUERY_STRING", RACK_KEY.QUERY_STRING);
        putRack("SERVER_NAME", RACK_KEY.SERVER_NAME);
        putRack("SERVER_PORT", RACK_KEY.SERVER_PORT);
        putRack("CONTENT_TYPE", RACK_KEY.CONTENT_TYPE);
        putRack("REQUEST_URI", RACK_KEY.REQUEST_URI);
        putRack("REMOTE_ADDR", RACK_KEY.REMOTE_ADDR);
        putRack("rack.url_scheme", RACK_KEY.URL_SCHEME);
        putRack("rack.version", RACK_KEY.VERSION);
        putRack("rack.multithread", RACK_KEY.MULTITHREAD);
        putRack("rack.multiprocess", RACK_KEY.MULTIPROCESS);
        putRack("rack.run_once", RACK_KEY.RUN_ONCE);
        putRack("CONTENT_LENGTH", RACK_KEY.CONTENT_LENGTH);
        putRack("HTTPS", RACK_KEY.HTTPS);
    }

    private void putRack(String key, RACK_KEY value) {
        rackKeyMap.put(RubyHelper.toUsAsciiRubyString(runtime, key), value);
    }

    public RubyHash getEnv(final HttpServerRequest request,
                           final RackInput input,
                           final boolean isSSL) throws IOException {
        // XXX
        String contextPath = "/";
        MultiMap headers = request.headers();
        // TODO: Should we only use this faster RackEnvironmentHash if we detect
        // specific JRuby versions that we know are compatible?
        final RackEnvironmentHash env = new RackEnvironmentHash(runtime, headers, rackKeyMap);
        env.lazyPut(RACK_KEY.RACK_INPUT, input, false);
        env.lazyPut(RACK_KEY.RACK_ERRORS, errors, false);

        // Don't use request.getPathInfo because that gets decoded by the container
        String pathInfo = request.path();

        // strip contextPath and servletPath from pathInfo
        if (pathInfo.startsWith(contextPath) && !contextPath.equals("/")) {
            pathInfo = pathInfo.substring(contextPath.length());
        }

        String scriptName = contextPath;
        // SCRIPT_NAME should be an empty string for the root
        if (scriptName.equals("/")) {
            scriptName = "";
        }

        env.lazyPut(RACK_KEY.REQUEST_METHOD, request.method(), true);
        env.lazyPut(RACK_KEY.SCRIPT_NAME, scriptName, false);
        env.lazyPut(RACK_KEY.PATH_INFO, pathInfo, false);
        env.lazyPut(RACK_KEY.QUERY_STRING, request.query() + "", false);
        env.lazyPut(RACK_KEY.SERVER_NAME, Const.LOCALHOST, false);
        env.lazyPut(RACK_KEY.SERVER_PORT, Const.PORT_80, true);
        env.lazyPut(RACK_KEY.CONTENT_TYPE, headers.get(HttpHeaders.Names.CONTENT_TYPE), true);
        env.lazyPut(RACK_KEY.REQUEST_URI, scriptName + pathInfo, false);
        env.lazyPut(RACK_KEY.REMOTE_ADDR, getRemoteAddr(request), true);
        env.lazyPut(RACK_KEY.URL_SCHEME, isSSL? Const.HTTPS : Const.HTTP, true);
        env.lazyPut(RACK_KEY.VERSION, rackVersion, false);
        env.lazyPut(RACK_KEY.MULTITHREAD, runtime.getTrue(), false);
        env.lazyPut(RACK_KEY.MULTIPROCESS, runtime.getFalse(), false);
        env.lazyPut(RACK_KEY.RUN_ONCE, runtime.getFalse(), false);


        final int contentLength = getContentLength(headers);
        if (contentLength >= 0) {
            env.lazyPut(RACK_KEY.CONTENT_LENGTH, contentLength + "", true);
        }

        if (isSSL) {
            env.lazyPut(RACK_KEY.HTTPS, "on", true);
        }

        return env;
    }

    private static String getRemoteAddr(final HttpServerRequest request) {
        InetSocketAddress sourceAddress = request.remoteAddress();
        if(sourceAddress == null) {
            return "";
        }
        InetAddress address = sourceAddress.getAddress();
        if(address == null) {
            return "";
        }
        return address.getHostAddress();
    }

    private static int getContentLength(final MultiMap headers) {
        final String contentLengthStr = headers.get(HttpHeaders.Names.CONTENT_LENGTH);
        if (contentLengthStr == null || contentLengthStr.isEmpty()) {
            return -1;
        }
        return Integer.parseInt(contentLengthStr);
    }

    private final Ruby runtime;
    private final RubyArray rackVersion;
    private final RubyIO errors;
    private final Map<RubyString, RACK_KEY> rackKeyMap = new HashMap<>();
}