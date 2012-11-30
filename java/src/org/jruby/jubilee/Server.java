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

import java.util.Map;

public class Server extends RubyObject {
    final private Vertx vertx;
    final private HttpServer httpServer;
    private RackApplication app;
    private boolean running;
    private RackInput emptyBody;
    private int port;
    public static void createServerClass(Ruby runtime) {
        RubyModule mJubilee = runtime.defineModule("Jubilee");
        RubyClass serverClass = mJubilee.defineClassUnder("VertxServer", runtime.getObject(), ALLOCATOR);
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
        this.app = new RackApplication(args[0]);
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
                app.call(new RackRequest(runtime, req)).respond(req.response);
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
                getRuntime().getOutputStream().println("closing.");
            }
        });
        return getRuntime().getNil();
    }
}
