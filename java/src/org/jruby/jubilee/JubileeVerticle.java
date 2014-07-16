package org.jruby.jubilee;

import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;
import org.vertx.java.platform.impl.WrappedVertx;

import java.io.IOException;

/**
 * Created by isaiah on 23/01/2014.
 */
public class JubileeVerticle extends Verticle {
  private Ruby runtime;

  @Override
  public void start() {
    JsonObject config = container.config();
    HttpServer httpServer = vertx.createHttpServer();
    runtime = createRuntime();
    IRubyObject rackApplication = initRackApplication(config);
    final RackApplication app;
    boolean ssl =config.getBoolean("ssl");
    try {
      app = new RackApplication((WrappedVertx) vertx, runtime.getCurrentContext(), rackApplication, ssl);
      httpServer.setAcceptBacklog(10000);
      httpServer.requestHandler(new Handler<HttpServerRequest>() {
        public void handle(final HttpServerRequest req) {
          app.call(req);
        }
      });
      if (config.containsField("event_bus")) {
        JsonArray allowAll = new JsonArray();
        allowAll.add(new JsonObject());
        JsonObject ebconf = new JsonObject();
        ebconf.putString("prefix", config.getString("event_bus"));
        vertx.createSockJSServer(httpServer).bridge(ebconf, allowAll, allowAll);
      }
      if (ssl) httpServer.setSSL(true).setKeyStorePath(config.getString("keystore_path"))
              .setKeyStorePassword(config.getString("keystore_password"));
      httpServer.listen(config.getInteger("port"), config.getString("host"));
    } catch (IOException e) {
      container.logger().fatal("Failed to create RackApplication");
    }
  }

  @Override
  public void stop() {
    this.runtime.tearDown(false);
  }

  private Ruby createRuntime() {
      Ruby runtime;
      if (Ruby.isGlobalRuntimeReady()) {
          runtime = Ruby.getGlobalRuntime();
      } else {
          throw new RuntimeException("ruby runtime is not available!");
      }
      return runtime;
  }

  private IRubyObject initRackApplication(JsonObject config) {
      String rackup = config.getString("rackup");
      String rackScript = "require 'rack'\n" +
              "require 'jubilee'\n" +
              "app, _ = Rack::Builder.parse_file('" + rackup + "')\n";
      if (!config.getBoolean("quiet") && config.getString("environment").equals("development")) {
        rackScript += "logger = STDOUT\n" +
                "app = Rack::CommonLogger.new(app, logger)\n";
      }
      rackScript += "Jubilee::Application.new(app)\n";
      return runtime.evalScriptlet(rackScript);
  }
}
