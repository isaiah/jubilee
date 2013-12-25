package org.jruby.jubilee;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.jubilee.impl.DefaultRackEnvironment;
import org.jruby.jubilee.impl.RubyIORackInput;
import org.jruby.jubilee.impl.RubyNullIO;
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
    private Ruby runtime;
    private DefaultVertx vertx;
    private RubyClass rackIOInputClass;
    private RubyClass httpServerResponseClass;
    private RubyArray rackVersion;
    private RubyNullIO nullio;

    public RackApplication(Vertx vertx, ThreadContext context, IRubyObject app, boolean ssl) {
        this.app = app;
        this.ssl = ssl;
        this.vertx = (DefaultVertx) vertx;
        this.runtime = context.runtime;
        this.rackVersion = RubyArray.newArrayLight(runtime, RubyFixnum.one(runtime), RubyFixnum.four(runtime));
        // Memorize the ruby classes
        this.rackIOInputClass = (RubyClass) runtime.getClassFromPath("Jubilee::IORackInput");
        this.httpServerResponseClass = (RubyClass) runtime.getClassFromPath("Jubilee::HttpServerResponse");
        this.nullio = new RubyNullIO(runtime, (RubyClass) runtime.getClassFromPath("Jubilee::NullIO"));
    }

    public void call(final HttpServerRequest request) {
        String contentLength = request.headers().get(Const.Vertx.CONTENT_LENGTH);
        final RackInput input;
        if (contentLength != null && contentLength.equals("0"))
            input = nullio;
        else {
            final ByteBuf bodyBuf = Unpooled.buffer(0, Integer.MAX_VALUE);
            final AtomicBoolean eof = new AtomicBoolean(false);
            input = new RubyIORackInput(runtime, rackIOInputClass, request, bodyBuf, eof);

            request.dataHandler(new Handler<Buffer>() {
                @Override
                public void handle(Buffer buffer) {
                    bodyBuf.writeBytes(buffer.getByteBuf());
                }
            });

            request.endHandler(new VoidHandler() {
                @Override
                protected void handle() {
                    eof.set(true);
                }
            });
        }
        Runnable task = new Runnable() {
            @Override
            public void run() {
                RackEnvironment env = new DefaultRackEnvironment(runtime, request, input, ssl, rackVersion);
                // This is a different context, do NOT replace runtime.getCurrentContext()
                IRubyObject result = app.callMethod(runtime.getCurrentContext(), "call", env.getEnv());
                RackResponse response = (RackResponse) JavaEmbedUtils.rubyToJava(runtime, result, RackResponse.class);
                RubyHttpServerResponse resp = new RubyHttpServerResponse(runtime,
                        httpServerResponseClass,
                        request.response());
                response.respond(resp);
            }
        };
        vertx.startOnEventLoop(task);
    }

}
