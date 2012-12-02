package org.jruby.jubilee;


import org.vertx.java.core.*;
import org.vertx.java.core.http.*;

import org.jruby.*;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.*;
import org.jruby.anno.JRubyMethod;

import java.util.Map;

public class Server extends RubyObject {
  final private Vertx vertx;
  final private HttpServer httpServer;
  private RackApplication app;
  private boolean running;
  private boolean ssl = false;
  private String keyStorePath;
  private String keyStorePassword;
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

  @JRubyMethod(name = "initialize", required = 2, optional = 3)
  public IRubyObject initialize(ThreadContext context, IRubyObject[] args, Block block) {
    this.app = new RackApplication(args[0]);
    this.port = RubyInteger.num2int(args[1]);
    if (args.length == 3) {
      this.ssl = args[2].isTrue();
    }
    if (args.length == 4)
      this.keyStorePath = args[3].toString();
    if (args.length == 5)
      this.keyStorePassword = args[4].toString();
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
        req.response.end("hello world");
        //app.call(new RackRequest(runtime, req)).respond(req.response);
      }
    });
    //if (ssl)
    httpServer.setSSL(true)
            .setKeyStorePath("/home/isaiah/codes/playground/jubilee/examples/jubilee/server-keystore.jks")
            .setKeyStorePassword("wibble");
    httpServer.listen(this.port);
    return this;
  }

  @JRubyMethod(name = "persistent_timeout=")
  public IRubyObject setPersistentTimeout(final ThreadContext context, final IRubyObject timeout) {
    httpServer.setPersistentTimeout(RubyInteger.fix2long(timeout));
    return this;
  }

  @JRubyMethod(name = {"stop", "close"})
  public IRubyObject close(ThreadContext context) {
    this.running = false;
    httpServer.close();
//        httpServer.close(new SimpleHandler() {
//            @Override
//            protected void handle() {
//                getRuntime().getOutputStream().println("closing.");
//            }
//        });
    return getRuntime().getNil();
  }
}
