package org.jruby.jubilee.impl;

import org.jruby.RubyArray;
import org.jruby.RubyString;
import org.jruby.jubilee.RackEnvironment;
import org.jruby.jubilee.Const;
import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.jubilee.RackInput;
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
  private Ruby runtime;

  public DefaultRackEnvironment(final Ruby runtime, final HttpServerRequest request, RackInput input, boolean isSSL) {

    this.runtime = runtime;
    // DEFAULT
    env = RubyHash.newHash(runtime);
    env.put(Const.RACK_VERSION, Const.RackVersion(runtime));
    env.put(Const.RACK_ERRORS, new RubyIORackErrors(runtime));
    env.put(Const.RACK_MULTITHREAD, true);
    env.put(Const.RACK_MULTIPROCESS, false);
    env.put(Const.RACK_RUNONCE, true);
    if (isSSL)
      env.put(Const.URL_SCHEME, Const.HTTPS);
    else
      env.put(Const.URL_SCHEME, Const.HTTP);
    env.put(Const.SCRIPT_NAME, "");

    // Rack blows up if this is an empty string, and Rack::Lint
    // blows up if it's nil. So 'text/plain' seems like the most
    // sensible default value.
    //env.put(Const.HTTP_CONTENT_TYPE, "text/plain");

    env.put(Const.SERVER_PROTOCOL, Const.HTTP_11);
    env.put(Const.SERVER_SOFTWARE, Const.JUBILEE_VERSION);
    env.put(Const.GATEWAY_INTERFACE, Const.CGI_VER);

    // Parse request headers
    Map<String, String> headers = request.headers();
    String host;
    if ((host = headers.get(Const.HTTP_HOST)) != null) {
      int colon = host.indexOf(":");
      if (colon > 0) {
        env.put(Const.SERVER_NAME, host.substring(0, colon));
        env.put(Const.SERVER_PORT, host.substring(colon + 1));
      } else {
        env.put(Const.SERVER_NAME, host);
        env.put(Const.SERVER_PORT, Const.PORT_80);
      }

    } else {
      env.put(Const.SERVER_NAME, Const.LOCALHOST);
      env.put(Const.SERVER_PORT, Const.PORT_80);
    }

    env.put(Const.RACK_INPUT, input);
    env.put(Const.REQUEST_METHOD, request.method);
    env.put(Const.REQUEST_PATH, request.path);
    env.put(Const.REQUEST_URI, request.uri);
    env.put(Const.QUERY_STRING, orEmpty(request.query));
    env.put(Const.HTTP_HOST, headers.get("host"));
    env.put(Const.HTTP_COOKIE, orEmpty(headers.get("cookie")));
    env.put(Const.HTTP_USER_AGENT, headers.get("user-agent"));
    env.put(Const.HTTP_ACCEPT, headers.get("accept"));
    env.put(Const.HTTP_ACCEPT_LANGUAGE, orEmpty(headers.get("accept-language")));
    env.put(Const.HTTP_ACCEPT_ENCODING, orEmpty(headers.get("accept-encoding")));
    env.put(Const.HTTP_CONNECTION, orEmpty(headers.get("connection")));
    env.put(Const.HTTP_CONTENT_TYPE, orEmpty(headers.get("content-type")));
    String contentLength;
    if ((contentLength = headers.get("content-length")) != null)
      env.put(Const.HTTP_CONTENT_LENGTH, contentLength);
    env.put(Const.PATH_INFO, request.path);
  }

  public RubyHash getEnv() {
    return env;
  }

  private RubyString orEmpty(String jString) {
    return jString == null ? RubyString.newEmptyString(runtime) : RubyString.newString(runtime, jString);
  }
}
