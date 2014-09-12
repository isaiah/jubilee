package org.jruby.jubilee;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.jubilee.impl.RubyIORackInput;
import org.jruby.jubilee.impl.RubyNullIO;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.impl.WrappedVertx;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
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
    private boolean hideErrorStack;
    private Ruby runtime;
    private WrappedVertx vertx;
    private RubyClass rackIOInputClass;
    private RubyClass httpServerResponseClass;
    private RackEnvironment rackEnv;

    public RackApplication(WrappedVertx vertx, ThreadContext context, IRubyObject app, JsonObject config) throws IOException {
        this.app = app;
        this.ssl = config.getBoolean("ssl", false);
        this.hideErrorStack = config.getBoolean("hide_error_stack", false);
        this.vertx = vertx;
        this.runtime = context.runtime;
        // Memorize the ruby classes
        this.rackIOInputClass = (RubyClass) runtime.getClassFromPath("Jubilee::IORackInput");
        this.httpServerResponseClass = (RubyClass) runtime.getClassFromPath("Jubilee::HttpServerResponse");

        this.rackEnv = new RackEnvironment(runtime);
    }

    public void call(final HttpServerRequest request) {
//        String te = request.headers().get(Const.Vertx.TRANSFER_ENCODING);
//        String contentLength;
        final RackInput input;
        // This should be handled by Netty (expose a contentLength methods via HttpRequest,
        // it set the empty content flag when passing the header
//        if ((te != null && te.equals("chunked")) ||
//                ((contentLength = request.headers().get(Const.Vertx.CONTENT_LENGTH)) != null && !contentLength.equals("0"))) {
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
        request.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable ignore) {
                eof.set(true);
            }
        });
//        } else {
//            input = nullio;
//        }
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    // This is a different context, do NOT replace runtime.getCurrentContext()
                    IRubyObject result = app.callMethod(runtime.getCurrentContext(), "call", rackEnv.getEnv(request, input, ssl));
                    /*
                    if (request.isHijacked()) {
                        // It's the hijacker's response to close the socket.
                        return;
                    }
                    */
                    RackResponse response = (RackResponse) JavaEmbedUtils.rubyToJava(runtime, result, RackResponse.class);
                    RubyHttpServerResponse resp = new RubyHttpServerResponse(runtime,
                            httpServerResponseClass, request);
                    response.respond(resp);
                } catch (Exception e) {
                    request.response().setStatusCode(500);
                    String message = "Jubilee caught this error: " + e.getMessage() + "\n";
                    StringWriter stringWriter = new StringWriter();
                    PrintWriter printWriter = new PrintWriter(stringWriter);
                    e.printStackTrace(printWriter);
                    if (hideErrorStack) {
                        request.response().end("Internal error.");
                    } else {
                        request.response().end(message + stringWriter.toString());
                    }
                    e.printStackTrace(runtime.getErrorStream());
                }
            }
        };
        vertx.startInBackground(task, false);
    }

}
