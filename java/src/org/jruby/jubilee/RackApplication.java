package org.jruby.jubilee;

import org.jruby.Ruby;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;
import org.vertx.java.core.http.HttpServerRequest;

/**
 * Created with IntelliJ IDEA.
 * User: isaiah
 * Date: 11/29/12
 * Time: 5:40 PM
 */
public class RackApplication {
  private IRubyObject app;
  private boolean ssl;

  public RackApplication(IRubyObject app, boolean ssl) {
    this.app = app;
    this.ssl = ssl;
  }

  public RackResponse call(HttpServerRequest req) {
    RackRequest request = new RackRequest(getRuntime(), req, ssl);
    IRubyObject result = app.callMethod(getRuntime().getCurrentContext(), "call", request.getRackEnv());
    return (RackResponse) JavaEmbedUtils.rubyToJava(getRuntime(), result, RackResponse.class);
  }

  public Ruby getRuntime() {
    return app.getRuntime();
  }
}
