package org.jruby.jubilee;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import javafx.application.Application;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.jubilee.impl.DefaultRackEnvironment;
import org.jruby.jubilee.impl.DefaultRackResponse;
import org.jruby.jubilee.impl.RubyIORackInput;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

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
  private ByteBuf bodyBuf;
  private ApplicationMgr mgr;

  private final AtomicBoolean eof;

  public RackApplication(ThreadContext context, IRubyObject app, boolean ssl, ApplicationMgr mgr) {
    this.app = app;
    this.context = context;
    this.ssl = ssl;
    this.mgr = mgr;

    this.bodyBuf = Unpooled.unreleasableBuffer(Unpooled.buffer(0, Integer.MAX_VALUE));
    this.eof = new AtomicBoolean(false);
  }

  public void call(final HttpServerRequest request) {
    this.bodyBuf.clear();
    this.eof.set(false);
    final Ruby runtime = this.context.runtime;
    request.dataHandler(new Handler<Buffer>() {
        @Override
        public void handle(Buffer buffer) {
            bodyBuf.writeBytes(buffer.getBytes());
        }
    });
    // TODO optimize by use NullIO when there is no body here.

        RackInput input = new RubyIORackInput(runtime, request, bodyBuf, eof);
        RackEnvironment env = new DefaultRackEnvironment(runtime, request, input, ssl);
        IRubyObject result = app.callMethod(runtime.getCurrentContext(), "call", env.getEnv());
        mgr.returnApp(this);
        RubyArray ary = result.convertToArray();
        RackResponse response = new DefaultRackResponse(context, ary.shift(context), ary.shift(context), ary.shift(context));
        response.respond(request.response());

    request.endHandler(new VoidHandler() {
      @Override
      protected void handle() {
        eof.set(true);
      }
    });
  }
}
