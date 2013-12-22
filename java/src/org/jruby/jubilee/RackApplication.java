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
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.impl.DefaultVertx;
import org.vertx.java.core.spi.Action;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
  private DefaultVertx vertx;

  public RackApplication(DefaultVertx vertx, ThreadContext context, IRubyObject app, boolean ssl, int numberOfWorkers) {
    this.app = app;
    this.ssl = ssl;
    this.context = context;
    this.vertx = vertx;
  }

  public void call(final HttpServerRequest request) {
      final ByteBuf bodyBuf = Unpooled.buffer(0, Integer.MAX_VALUE);
      final Ruby runtime = context.runtime;
      final AtomicBoolean eof = new AtomicBoolean(false);
      request.dataHandler(new Handler<Buffer>() {
          @Override
          public void handle(Buffer buffer) {
              bodyBuf.writeBytes(buffer.getByteBuf());
          }
      });
      // TODO optimize by use NullIO when there is no body here.
      RackInput input = new RubyIORackInput(runtime, request, bodyBuf, eof);
      final RackEnvironment env = new DefaultRackEnvironment(runtime, request, input, ssl);
      Action<IRubyObject> task = new Action<IRubyObject>() {
          @Override
          public IRubyObject perform() {
              return app.callMethod(runtime.getCurrentContext(), "call", env.getEnv());
          }
      };
      vertx.executeBlocking(task, new Handler<AsyncResult<IRubyObject>>() {
          @Override
          public void handle(AsyncResult<IRubyObject> result) {
              if (result.succeeded()) {
                  RackResponse response = (RackResponse) JavaEmbedUtils.rubyToJava(runtime, result.result(), RackResponse.class);
                  RubyHttpServerResponse resp = new RubyHttpServerResponse(runtime,
                          (RubyClass) runtime.getClassFromPath("Jubilee::HttpServerResponse"),
                          request.response());
                  response.respond(resp);
              } else {
                  // NOOP
              }
          }
      });
      request.endHandler(new VoidHandler() {
          @Override
          protected void handle() {
              eof.set(true);
          }
      });
  }

  public void shutdown(boolean force) {
//    if (force)
//      exec.shutdownNow();
//    else
//      exec.shutdown();
  }
}
