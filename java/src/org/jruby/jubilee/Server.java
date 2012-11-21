package ext.jubilee;

import org.vertx.java.core.*;
import org.vertx.java.core.buffer.*;
import org.vertx.java.core.http.*;

import org.jruby.*;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;

@JRubyClass(name = "server")
public class Server extends RubyObject {
    final private Vertx vertx;
    final private HttpServer httpServer;
    public static RubyClass createServerClass(Ruby runtime) {
        RubyClass serverClass = runtime.defineClass("Server", runtime.getObject(), ALLOCATOR);
        serverClass.setReifiedClass(Server.class);
        serverClass.includeModule(runtime.getEnumerable());
        serverClass.defineAnnotatedMethods(Server.class);
        return serverClass;
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

    @JRubyMethod(name = "request_handler")
    public IRubyObject addHandler(final ThreadContext context, final IRubyObject app) {
        final Ruby runtime = context.runtime;
        httpServer.requestHandler(new Handler<HttpServerRequest>() {
            public void handle(HttpServerRequest req) {
                final Buffer body = new Buffer(0);
                req.dataHandler(new Handler<Buffer>() {
                    public void handle(Buffer buffer) {
                        body.appendBuffer(buffer);
                    }
                });
                RubyHash env = RubyHash.newHash(runtime);
                IRubyObject[] args = new IRubyObject[] {env};
                IRubyObject result = app.callMethod(context, "call", args, Block.NULL_BLOCK);
                req.response.write("hello");
                req.response.end();
            }
        });
        return runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject normalize_env(IRubyObject headers) {
        return getRuntime().getNil();
    }
}
