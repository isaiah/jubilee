package org.jruby.jubilee;

import org.vertx.java.core.*;
import org.vertx.java.core.http.*;

import org.jruby.*;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.*;
import org.jruby.anno.JRubyMethod;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class Server extends RubyObject {
  final private Vertx vertx;
  final private HttpServer httpServer;
  private RackApplication app;
  private boolean running;
  private boolean ssl = false;
  private String keyStorePath;
  private String keyStorePassword;
  private String eventBusPrefix;
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
  }

  /**
   * Initialize jubilee server, take a rack application and a configuration hash as parameter
   * @param context
   * @param args
   * @param block
   * @return
   */
  @JRubyMethod(name = "initialize")
  public IRubyObject initialize(ThreadContext context, IRubyObject app, IRubyObject config, Block block) {
    Ruby runtime = getRuntime();
    RubyHash options = config.convertToHash();
    RubySymbol port_k = runtime.newSymbol("port");
    RubySymbol ssl_k = runtime.newSymbol("ssl");
    RubySymbol keystore_path_k = runtime.newSymbol("keystore_path");
    RubySymbol keystore_password_k = runtime.newSymbol("keystore_password");
    RubySymbol eventbus_prefix_k = runtime.newSymbol("eventbus_prefix");
    this.port = RubyInteger.num2int(options.op_aref(context, port_k));
    this.ssl = options.op_aref(context, ssl_k).isTrue();
    this.app = new RackApplication(app, this.ssl);
    if (options.has_key_p(keystore_path_k).isTrue()) {
      this.keyStorePath = options.op_aref(context, keystore_path_k).toString();
      this.keyStorePassword = options.op_aref(context, keystore_password_k).toString();
    }
    if (options.has_key_p(eventbus_prefix_k).isTrue()) {
      this.eventBusPrefix = options.op_aref(context, eventbus_prefix_k).toString();
    }
    running = false;
    return this;
  }

  /**
   * Start http server, initialize states
   * @param context
   * @param block
   * @return
   */
  @JRubyMethod(name = "start")
  public IRubyObject start(final ThreadContext context, final Block block) {
    this.running = true;
    httpServer.setAcceptBacklog(10000);
    httpServer.requestHandler(new Handler<HttpServerRequest>() {
      public void handle(final HttpServerRequest req) {
        app.call(req);
      }
    });
    if (eventBusPrefix != null) {
      JsonObject config = new JsonObject().putString("prefix", eventBusPrefix);
      // TODO read inbounds and outbounds from config file
      vertx.createSockJSServer(httpServer).bridge(config, new JsonArray(), new JsonArray());
    }
    if (ssl) httpServer.setSSL(true).setKeyStorePath(this.keyStorePath)
            .setKeyStorePassword(this.keyStorePassword);
    httpServer.listen(this.port);
    return this;
  }

  /**
   * Set timeout for keep alive connection
   * @param context
   * @param timeout (in TimeUnit.SECONDS)
   * @return this
   */
  @JRubyMethod(name = "persistent_timeout=")
  public IRubyObject setPersistentTimeout(final ThreadContext context, final IRubyObject timeout) {
    httpServer.setPersistentTimeout(RubyInteger.fix2long(timeout) * 1000);
    return this;
  }

  /**
   * Stop the HttpServer
   * @param context
   * @param args if shutdown abruptly
   * @param block callback on close
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
