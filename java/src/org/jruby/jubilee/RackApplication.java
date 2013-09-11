package org.jruby.jubilee;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jruby.Ruby;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.jubilee.impl.DefaultRackEnvironment;
import org.jruby.jubilee.impl.RubyIORackInput;
import org.jruby.runtime.builtin.IRubyObject;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

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

  private ExecutorService exec;

  public RackApplication(IRubyObject app, boolean ssl, int numberOfWorkers) {
    this.app = app;
    this.ssl = ssl;
    exec = Executors.newFixedThreadPool(numberOfWorkers);
  }

  public void call(final HttpServerRequest request) {
    final ByteBuf bodyBuf = Unpooled.unreleasableBuffer(Unpooled.buffer(0, Integer.MAX_VALUE));
    final Ruby runtime = app.getRuntime();
    final AtomicBoolean eof = new AtomicBoolean(false);
    request.dataHandler(new Handler<Buffer>() {
      @Override
      public void handle(Buffer buffer) {
        bodyBuf.writeBytes(buffer.getBytes());
      }
    });
    // TODO optimize by use NullIO when there is no body here.
    Runnable task = new Runnable() {
      @Override
      public void run() {
        RackInput input = new RubyIORackInput(runtime, request, bodyBuf, eof);
        RackEnvironment env = new DefaultRackEnvironment(runtime, request, input, ssl);
        IRubyObject result = app.callMethod(runtime.getCurrentContext(), "call", env.getEnv());
        RackResponse response = (RackResponse) JavaEmbedUtils.rubyToJava(runtime, result, RackResponse.class);
        response.respond(request.response());
      }
    };
    exec.execute(task);
    request.endHandler(new VoidHandler() {
      @Override
      protected void handle() {
        eof.set(true);
      }
    });
  }

  public void shutdown(boolean force) {
    if (force)
      exec.shutdownNow();
    else
      exec.shutdown();
  }
}
