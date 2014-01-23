package org.jruby.jubilee;

import org.jruby.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.jubilee.vertx.JubileeVertx;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.impl.WrappedVertx;

import java.io.IOException;

public class RubyServer extends RubyObject {
    private Vertx vertx;
    private HttpServer httpServer;
    private RackApplication app;
    private boolean running = false;
    private boolean ssl = false;
    private String keyStorePath;
    private String keyStorePassword;
    private String eventBusPrefix;
    private int port;
    private String host;

    public static void createServerClass(Ruby runtime) {
        RubyModule mJubilee = runtime.defineModule("Jubilee");
        RubyClass serverClass = mJubilee.defineClassUnder("VertxServer", runtime.getObject(), ALLOCATOR);
        serverClass.defineAnnotatedMethods(RubyServer.class);
    }

    private static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby ruby, RubyClass rubyClass) {
            return new RubyServer(ruby, rubyClass);
        }
    };

    public RubyServer(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }

    /**
     * Initialize jubilee server, take a rack application and a configuration hash as parameter
     *
     * @param context
     * @param app
     * @param config
     * @param block
     * @return
     */
    @JRubyMethod(name = "initialize")
    public IRubyObject initialize(ThreadContext context, IRubyObject app, IRubyObject config, Block block) {
        Ruby runtime = getRuntime();
        /* configuration keys */
        RubyHash options = config.convertToHash();
        RubySymbol port_k = runtime.newSymbol("Port");
        RubySymbol host_k = runtime.newSymbol("Host");
        RubySymbol ssl_k = runtime.newSymbol("ssl");
        RubySymbol ssl_keystore_k = runtime.newSymbol("ssl_keystore");
        RubySymbol ssl_password_k = runtime.newSymbol("ssl_password");
        RubySymbol eventbus_prefix_k = runtime.newSymbol("eventbus_prefix");

        /* retrieve from passed in options */
        this.port = RubyNumeric.num2int(options.op_aref(context, port_k));
        this.host = options.op_aref(context, host_k).asJavaString();

        this.ssl = options.op_aref(context, ssl_k).isTrue();
        if (this.ssl) {
            this.keyStorePath = options.op_aref(context, ssl_keystore_k).asJavaString();
            if (options.has_key_p(ssl_password_k).isTrue())
                this.keyStorePassword = options.op_aref(context, ssl_password_k).asJavaString();
        }
        if (options.has_key_p(eventbus_prefix_k).isTrue())
            this.eventBusPrefix = options.op_aref(context, eventbus_prefix_k).asJavaString();

        this.vertx = JubileeVertx.vertx();

        httpServer = vertx.createHttpServer();
        try {
            this.app = new RackApplication((WrappedVertx) vertx, context, app, this.ssl);
            if (block.isGiven()) block.yieldSpecific(context, this);
        } catch (IOException e) {
            // noop
        }
        return this;
    }

    /**
     * Start http server, initialize states
     *
     * @param context
     * @param block
     * @return
     */
    @JRubyMethod(name = "start")
    public IRubyObject start(final ThreadContext context, final Block block) {
        httpServer.setAcceptBacklog(10000);
        httpServer.requestHandler(new Handler<HttpServerRequest>() {
            public void handle(final HttpServerRequest req) {
                app.call(req);
            }
        });
        if (eventBusPrefix != null) {
            JsonObject config = new JsonObject().putString("prefix", eventBusPrefix);
            JsonArray allowAll = new JsonArray();
            allowAll.add(new JsonObject());
            // TODO read inbounds and outbounds from config file
            vertx.createSockJSServer(httpServer).bridge(config, allowAll, allowAll);
        }
        if (ssl) httpServer.setSSL(true).setKeyStorePath(this.keyStorePath)
                .setKeyStorePassword(this.keyStorePassword);
        httpServer.listen(this.port, this.host);
        this.running = true;
        if (block.isGiven()) block.yieldSpecific(context, this);
        return this;
    }

    /**
     * Set timeout for keep alive connection
     *
     * @param context
     * @param timeout (in TimeUnit.SECONDS)
     * @return this
     */
    @JRubyMethod(name = "persistent_timeout=")
    public IRubyObject setPersistentTimeout(final ThreadContext context, final IRubyObject timeout) {
        // FIXME
        //httpServer.setPersistentTimeout(RubyInteger.fix2long(timeout) * 1000);
        return this;
    }

    /**
     * Stop the HttpServer
     *
     * @param context
     * @param args    if shutdown abruptly
     * @param block   callback on close
     * @return
     */
    @JRubyMethod(name = {"stop", "close"}, optional = 1)
    public IRubyObject close(ThreadContext context, IRubyObject[] args, Block block) {
        if (running) {
            this.running = false;
            httpServer.close();
            // DO I need to stop?
            //vertx.stop();
            if (block.isGiven()) block.yieldSpecific(context);
        } else {
            getRuntime().getOutputStream().println("jubilee server not running?");
        }
        return getRuntime().getNil();
    }
}
