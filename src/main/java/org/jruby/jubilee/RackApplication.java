package org.jruby.jubilee;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.jubilee.impl.RubyIORackInput;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

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
    private Vertx vertx;
    private RubyClass rackIOInputClass;
    private RubyClass httpServerResponseClass;
    private RackEnvironment rackEnv;
    private boolean multiThreaded = true;

    public RackApplication(Vertx vertx, ThreadContext context, IRubyObject app, JsonObject config) throws IOException {
        this.app = app;
        this.ssl = config.getBoolean("ssl");
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

        request.handler(buffer -> {
            bodyBuf.writeBytes(buffer.getByteBuf());
        });

        request.endHandler(v -> {
            eof.set(true);
        });
        request.exceptionHandler(ignore -> {
            eof.set(true);
        });
//        } else {
//            input = nullio;
//        }
        vertx.executeBlocking((future) -> {
            // This is a different context, do NOT replace runtime.getCurrentContext()
            ThreadContext context = runtime.getCurrentContext();
            RubyHash env = null;
            try {
                env = rackEnv.getEnv(request, input, ssl);
            } catch (IOException e) {
                fail(e, request);
            }
            IRubyObject result = app.callMethod(context, "call", env);
//                if (request.isHijacked()) {
//                    // It's the hijacker's response to close the socket.
//                    return;
//                }

            boolean multiThreaded = rackEnv.isMutliThreaded();
            if (multiThreaded) {
                future.complete(result);
            } else {
                respond(result, request);
                runtime.getOutputStream().println("not multiple threaded");
            }
        }, false, (ar) -> {
            if (ar.succeeded()) {
                runtime.getOutputStream().println("multiple threaded");
                respond((IRubyObject) ar.result(), request);
            } else {
                fail(ar.cause(), request);
            }
        });
    }

    private void respond(final IRubyObject result, final HttpServerRequest request) {
        RubyHttpServerResponse resp = new RubyHttpServerResponse(runtime,
                httpServerResponseClass, request);
        RackResponse response = (RackResponse) JavaEmbedUtils.rubyToJava(runtime, result, RackResponse.class);
        response.respond(resp);
    }

    private void fail(final Throwable e, final HttpServerRequest request) {
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
    }

}
