package org.jruby.jubilee;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.jubilee.impl.DefaultRackEnvironment;
import org.jruby.jubilee.impl.RubyIORackInput;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.impl.DefaultVertx;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created with IntelliJ IDEA.
 * User: isaiah
 * Date: 11/29/12
 * Time: 5:40 PM
 */
public class RackApplication {
  private IRubyObject app;
  private boolean ssl;
  private ThreadContext context;
  private Ruby runtime;
  private DefaultVertx vertx;
  private RubyClass rackIOInputClass;
  private RubyClass httpServerResponseClass;

  public RackApplication(Vertx vertx, ThreadContext context, IRubyObject app, boolean ssl) {
    this.app = app;
    this.ssl = ssl;
    this.context = context;
    this.vertx = (DefaultVertx) vertx;
    this.runtime = context.runtime;
    // Memorize the ruby classes
    this.rackIOInputClass = (RubyClass) runtime.getClassFromPath("Jubilee::IORackInput");
    this.httpServerResponseClass = (RubyClass) runtime.getClassFromPath("Jubilee::HttpServerResponse");
  }

  public void call(final HttpServerRequest request) {
    final ByteBuf bodyBuf = Unpooled.buffer();
    final AtomicBoolean eof = new AtomicBoolean(false);
    request.dataHandler(new Handler<Buffer>() {
      @Override
      public void handle(Buffer buffer) {
        bodyBuf.writeBytes(buffer.getByteBuf());
      }
    });
    Runnable task = new Runnable() {
      @Override
      public void run() {
        RackInput input = new RubyIORackInput(runtime, rackIOInputClass, request, bodyBuf, eof);
        RackEnvironment env = new DefaultRackEnvironment(runtime, request, input, ssl);
        IRubyObject result = app.callMethod(runtime.getCurrentContext(), "call", env.getEnv());
        RackResponse response = (RackResponse) JavaEmbedUtils.rubyToJava(runtime, result, RackResponse.class);
          RubyHttpServerResponse resp = new RubyHttpServerResponse(runtime,
                  httpServerResponseClass,
                  request.response());
        response.respond(resp);
      }
    };
    vertx.startOnEventLoop(task);
    request.endHandler(new VoidHandler() {
      @Override
      protected void handle() {
        eof.set(true);
      }
    });
  }

}
