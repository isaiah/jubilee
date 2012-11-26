package org.jruby.jubilee;

import org.jruby.jubilee.impl.DefaultRackEnvironment;
import org.vertx.java.core.*;
import org.vertx.java.core.buffer.*;
import org.vertx.java.core.http.*;

import org.jruby.*;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;

public class Server extends RubyObject {
    final private Vertx vertx;
    final private HttpServer httpServer;
    private IRubyObject app;
    public static void createServerClass(Ruby runtime) {
        RubyModule mJubilee = runtime.defineModule("Jubilee");
        RubyClass serverClass = mJubilee.defineClassUnder("Server", runtime.getObject(), ALLOCATOR);
        serverClass.defineAnnotatedMethods(Server.class);
    }

    private static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby ruby, RubyClass rubyClass) {
            return new Server(ruby, rubyClass);
        }
    };

    public Server(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
        vertx = Vertx.newVertx();
        httpServer = vertx.createHttpServer();
    }

    @JRubyMethod(name = "request_handler", optional = 1)
    public IRubyObject addHandler(final ThreadContext context, final IRubyObject[] args, final Block block) {
        app = callMethod(context, "load_rack_adapter", args, block);
        final Ruby runtime = context.runtime;
        httpServer.requestHandler(new Handler<HttpServerRequest>() {
            public void handle(final HttpServerRequest req) {
                final Buffer body = new Buffer(0);
                req.dataHandler(new Handler<Buffer>() {
                    public void handle(Buffer buffer) {
                        body.appendBuffer(buffer);
                    }
                });

                RubyHash env = new DefaultRackEnvironment(runtime, req).getEnv();
                IRubyObject[] args = new IRubyObject[] {env};
                RubyArray result = (RubyArray) app.callMethod(context, "call", args, Block.NULL_BLOCK);
                int status = RubyInteger.num2int(result.entry(0));
                RubyHash headers = (RubyHash) result.entry(1);
                req.response.statusCode = status;
                for (IRubyObject key : headers.keys().toJavaArray()) {
                    req.response.putHeader(key.toString(), headers.op_aref(context, key).toString());
                }

                IRubyObject respBody = result.entry(2);
                if (! respBody.respondsTo("each"))
                    throw new RuntimeException("rack response should respond to each");
                final Buffer buffer = new Buffer();
                // TODO: support chunked response
                RubyEnumerable.callEach(runtime, context, respBody, new BlockCallback(){
                    @Override
                    public IRubyObject call(ThreadContext threadContext, IRubyObject[] args, Block block) {
                        buffer.appendString(args[0].toString(), "UTF-8");
                        return runtime.getNil();
                    }
                });
                req.response.putHeader(Const.HTTP_CONTENT_LENGTH, buffer.length());

                req.response.write(buffer);
                req.response.end();
                req.response.close();
            }
        });
        httpServer.listen(3212);
        return this;
    }

    @JRubyMethod(name = {"stop", "close"})
    public IRubyObject close(ThreadContext context) {
        httpServer.close(new SimpleHandler() {
            @Override
            protected void handle() {
                getRuntime().getOutputStream().println("jubilee server closing.");
            }
        });
        return getRuntime().getNil();
    }
}
