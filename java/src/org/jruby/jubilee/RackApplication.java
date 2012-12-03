package org.jruby.jubilee;

import org.jruby.Ruby;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.jubilee.impl.DefaultRackEnvironment;
import org.jruby.jubilee.impl.NullIO;
import org.jruby.jubilee.impl.RubyIORackInput;
import org.jruby.runtime.builtin.IRubyObject;
import org.vertx.java.core.Handler;
import org.vertx.java.core.SimpleHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created with IntelliJ IDEA.
 * User: isaiah
 * Date: 11/29/12
 * Time: 5:40 PM
 */
public class RackApplication {
  private IRubyObject app;
  private boolean ssl;
  private BlockingQueue<String> bodyBuf;
  private int DEFAULT_CAPACITY = 10;

  public RackApplication(IRubyObject app, boolean ssl) {
    this.app = app;
    this.ssl = ssl;
    bodyBuf = new LinkedBlockingQueue<String>(DEFAULT_CAPACITY);
  }

  public void call(final HttpServerRequest request) {
    final Ruby runtime = app.getRuntime();
    request.dataHandler(new Handler<Buffer>() {
      @Override
      public void handle(Buffer buffer) {
        runtime.getOutputStream().println("========dataHandler======");
        runtime.getOutputStream().println(buffer.toString());
        runtime.getOutputStream().println("========dataHandler======");
        bodyBuf.offer(buffer.toString());
      }
    });
    // TODO optimize by use NullIO when there is no body here.
    RackInput input = new RubyIORackInput(runtime, bodyBuf);
    RackEnvironment env = new DefaultRackEnvironment(runtime, request, input, ssl);
    IRubyObject result = app.callMethod(runtime.getCurrentContext(), "call", env.getEnv());
    RackResponse response = (RackResponse) JavaEmbedUtils.rubyToJava(runtime, result, RackResponse.class);
    response.respond(request.response);
    request.endHandler(new SimpleHandler() {
      @Override
      protected void handle() {
        bodyBuf.offer(Const.END_OF_BODY);
        //RackInput input = body.length() == 0 ? new NullIO(runtime) : new RubyIORackInput(runtime, body);
        //RackEnvironment env = new DefaultRackEnvironment(runtime, request, input, ssl);
        //IRubyObject result = app.callMethod(runtime.getCurrentContext(), "call", env.getEnv());
        //RackResponse response = (RackResponse) JavaEmbedUtils.rubyToJava(runtime, result, RackResponse.class);
        //response.respond(request.response);
      }
    });
  }
}
