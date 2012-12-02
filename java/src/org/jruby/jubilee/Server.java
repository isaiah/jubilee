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
    this.port = RubyInteger.num2int(args[1]);
    if (args.length > 2) {
      this.ssl = args[2].isTrue();
    }
    this.app = new RackApplication(args[0], this.ssl);
    if (args.length > 3)
      this.keyStorePath = args[3].toString();
    if (args.length > 4)
      this.keyStorePassword = args[4].toString();
    running = false;
    return this;
  }

  @JRubyMethod(name = "start", optional = 1)
  public IRubyObject start(final ThreadContext context, final IRubyObject[] args, final Block block) {
    this.running = true;
    httpServer.requestHandler(new Handler<HttpServerRequest>() {
      public void handle(HttpServerRequest req) {
        app.call(req).respond(req.response);
      }
    });
    if (ssl) httpServer.setSSL(true).setKeyStorePath(this.keyStorePath)
            .setKeyStorePassword(this.keyStorePassword);
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
    if (running) {
      this.running = false;
      httpServer.close();
//        httpServer.close(new SimpleHandler() {
//            @Override
//            protected void handle() {
//                getRuntime().getOutputStream().println("closing.");
//            }
//        });
    } else {
      getRuntime().getOutputStream().println("not running?");
      // no op
    }
    return getRuntime().getNil();
  }
}
