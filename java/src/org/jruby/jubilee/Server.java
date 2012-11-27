package org.jruby.jubilee;

import org.jruby.jubilee.impl.DefaultRackEnvironment;
import org.jruby.jubilee.impl.NullIO;
import org.jruby.jubilee.impl.RubyIORackInput;
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
    private boolean running;
    private RackInput emptyBody;
    private int port;
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
        emptyBody = new NullIO(ruby);
    }

    @JRubyMethod(name = "initialize", required = 1, optional = 1)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args, Block block) {
        this.app = args[0];
        if (args.length == 2) {
            this.port = RubyInteger.num2int(args[1].convertToInteger());
        } else {
            this.port = 3212;
        }
        running = false;
        return this;
    }

    @JRubyMethod(name = "start", optional = 1)
    public IRubyObject start(final ThreadContext context, final IRubyObject[] args, final Block block) {
        final Ruby runtime = context.runtime;
        if (running) {
            runtime.newRuntimeError("Jubilee server is already running");
        }
        running = true;
        httpServer.requestHandler(new Handler<HttpServerRequest>() {
            public void handle(final HttpServerRequest req) {
                String contentLength = req.headers().get("Content-Length");
                RackInput input;
                if (contentLength == null) {
                    input = emptyBody;
                } else {
                    final Buffer body = new Buffer(0);
                    req.dataHandler(new Handler<Buffer>() {
                        public void handle(Buffer buffer) {
                            body.appendBuffer(buffer);
                        }
                    });
                    input = new RubyIORackInput(getRuntime(), body);
                }

                RubyHash env = new DefaultRackEnvironment(runtime, req, input).getEnv();
//                IRubyObject[] args = new IRubyObject[] {env};
                RubyArray result = (RubyArray) app.callMethod(context, "call", env);
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
            }
        });
        httpServer.listen(this.port);
        return this;
    }

    @JRubyMethod(name = {"stop", "close"})
    public IRubyObject close(ThreadContext context) {
        this.running = false;
        httpServer.close(new SimpleHandler() {
            @Override
            protected void handle() {
                getRuntime().getOutputStream().println("jubilee server closing.");
            }
        });
        return getRuntime().getNil();
    }
}
