package org.jruby.jubilee;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyInstanceConfig;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;
import org.jruby.runtime.builtin.IRubyObject;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;
import org.vertx.java.platform.impl.JRubyVerticleFactory;
import org.vertx.java.platform.impl.WrappedVertx;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by isaiah on 23/01/2014.
 */
public class JubileeVerticle extends Verticle {

    public JubileeVerticle(){}

    public JubileeVerticle(String rackup) {
        this.rackup = rackup;
    }

    @Override
    public void start() {
        // This is setup for the ruby apishims
        JRubyVerticleFactory.vertx = vertx;
        JRubyVerticleFactory.container = container;

        JsonObject config = container.config();
        HttpServer httpServer = vertx.createHttpServer();
        String root = config.getString("root", ".");
        this.scontainer =  new ScriptingContainer(LocalContextScope.CONCURRENT);
        String jrubyHome = config.getString("jruby-home", "");
        if (jrubyHome != null)
            this.scontainer.setHomeDirectory(jrubyHome);
        String expandedRoot = (String) this.scontainer.runScriptlet("File.expand_path(%q(" + root + "))");
        this.scontainer.setCurrentDirectory(expandedRoot);
        IRubyObject rackApplication = initRackApplication(config);
        final RackApplication app;
        boolean ssl = config.getBoolean("ssl");
        try {
            app = new RackApplication((WrappedVertx) vertx, rackApplication.getRuntime().getCurrentContext(), rackApplication, config);
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
        this.scontainer.clear();
        Ruby.clearGlobalRuntime();
    }

    private IRubyObject initRackApplication(JsonObject config) {
        if (this.rackup == null)
            this.rackup = config.getString("rackup");
        String rackScript = "require 'rack'\n" +
                "require 'jubilee'\n" +
                "app, _ = Rack::Builder.parse_file('" + rackup + "')\n";
        if (!config.getBoolean("quiet") && config.getString("environment").equals("development")) {
            rackScript += "logger = STDOUT\n" +
                    "app = Rack::CommonLogger.new(app, logger)\n";
        }
        rackScript += "Jubilee::Application.new(app)\n";
        return (IRubyObject) scontainer.runScriptlet(new StringReader(rackScript), rackup);
    }

    private ScriptingContainer scontainer;
    private String rackup;
}
