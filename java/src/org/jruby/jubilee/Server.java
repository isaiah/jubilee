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

public class Server extends RubyObject {
    private Vertx vertx;
    private HttpServer httpServer;
    private RackApplication app;
    private boolean running = false;
    private boolean ssl = false;
    private String keyStorePath;
    private String keyStorePassword;
    private String eventBusPrefix;
    private int numberOfWorkers;
    private int port;
    private String host;
    private int clusterPort;
    private String clusterHost;

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
    }

    /**
     * Initialize jubilee server, take a rack application and a configuration hash as parameter
     *
     * @param context
     * @param args
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
        RubySymbol cluster_port_k = runtime.newSymbol("cluster_port");
        RubySymbol cluster_host_k = runtime.newSymbol("cluster_host");
        RubySymbol ssl_k = runtime.newSymbol("ssl");
        RubySymbol keystore_path_k = runtime.newSymbol("keystore_path");
        RubySymbol keystore_password_k = runtime.newSymbol("keystore_password");
        RubySymbol eventbus_prefix_k = runtime.newSymbol("eventbus_prefix");
        RubySymbol number_of_workers_k = runtime.newSymbol("number_of_workers");

        /* retrieve from passed in options */
        this.port = Integer.parseInt(options.op_aref(context, port_k).toString());
        this.host = options.op_aref(context, host_k).toString();

        this.ssl = options.op_aref(context, ssl_k).isTrue();
        this.numberOfWorkers = Integer.parseInt(options.op_aref(context, number_of_workers_k).toString());
        if (options.has_key_p(keystore_path_k).isTrue()) {
            this.keyStorePath = options.op_aref(context, keystore_path_k).toString();
            this.keyStorePassword = options.op_aref(context, keystore_password_k).toString();
        }
        this.app = new RackApplication(app, this.ssl, this.numberOfWorkers);
        if (options.has_key_p(eventbus_prefix_k).isTrue())
            this.eventBusPrefix = options.op_aref(context, eventbus_prefix_k).toString();

        /* init vertx */
        if (options.has_key_p(cluster_host_k).isTrue()) {
            this.clusterHost = options.op_aref(context, cluster_host_k).toString();
            if (options.has_key_p(cluster_port_k).isTrue()) {
                this.clusterPort = Integer.parseInt(options.op_aref(context, cluster_port_k).toString());
                this.vertx = JubileeVertx.init(clusterPort, clusterHost);
            }
            this.vertx = JubileeVertx.init(clusterHost);
        } else {
            this.vertx = JubileeVertx.init();
        }

        httpServer = vertx.createHttpServer();
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
            if (args.length == 1)
                app.shutdown(args[0].isTrue());
            else
                app.shutdown(false);

            this.running = false;
            httpServer.close();
            if (block.isGiven()) block.yieldSpecific(context);
        } else {
            getRuntime().getOutputStream().println("jubilee server not running?");
        }
        return getRuntime().getNil();
    }
}
