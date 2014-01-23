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
  private Ruby ruby;

  @Override
  public void start() {
    JsonObject config = container.config();
    HttpServer httpServer = vertx.createHttpServer();
    ruby = config.getValue("ruby");
    IRubyObject rackApplication;
    final RackApplication app;
    boolean ssl =config.getBoolean("ssl");
    if (config.containsField("rackapp")) rackApplication = config.getValue("rackapp");
    else {
      String rackup = config.getString("rackup");
      String rackScript = "require 'rack'\n" +
              "require 'jubilee'\n" +
              "app, _ = Rack::Builder.parse_file('" + rackup + "')\n";
      if (config.containsField("quiet") && config.getString("environment").equals("development")) {
        rackScript += "logger = STDOUT\n" +
                "Rack::CommonLogger.new(@app, logger)";
      }
      rackScript += "Jubilee::Application.new(app)\n";
      rackApplication = ruby.evalScriptlet(rackScript);
    }
    try {
      app = new RackApplication((WrappedVertx) vertx, ruby.getCurrentContext(), rackApplication, ssl);
      httpServer.setAcceptBacklog(10000);
      httpServer.requestHandler(new Handler<HttpServerRequest>() {
        public void handle(final HttpServerRequest req) {
          app.call(req);
        }
      });
      if (config.containsField("event_bus")) {
        JsonArray allowAll = new JsonArray();
        allowAll.add(new JsonObject());
        vertx.createSockJSServer(httpServer).bridge(config.getObject("event_bus"), allowAll, allowAll);
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
    this.ruby.tearDown(false);
  }
}
