package org.jruby.jubilee;

import io.vertx.core.*;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyInstanceConfig;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by isaiah on 23/01/2014.
 */
public class JubileeVerticle extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        JsonObject config = config();
        HttpServerOptions httpServerOptions = new HttpServerOptions();
        httpServerOptions.setPort(config.getInteger("port"));
        httpServerOptions.setHost(config.getString("host"));
        httpServerOptions.setAcceptBacklog(10000);
        httpServerOptions.setClientAuthRequired(false);

        String root = config.getString("root", ".");
        this.runtime = createRuntime(root, config);
        String expandedRoot = this.runtime.evalScriptlet("File.expand_path(%q(" + root + "))").asJavaString();
        this.runtime.setCurrentDirectory(expandedRoot);
        IRubyObject rackApplication = initRackApplication(config);
        final RackApplication app;
        boolean ssl = config.getBoolean("ssl");
        if (ssl) {
            httpServerOptions.setSsl(true).setKeyStoreOptions(
                    new JksOptions().setPath(config.getString("keystore_path"))
                            .setPassword(config.getString("keystore_password"))
            );
        }
        Router router = Router.router(vertx);
        try {
            app = new RackApplication(vertx, runtime.getCurrentContext(), rackApplication, config);
            if (config.containsKey("event_bus")) {
                BridgeOptions options = new BridgeOptions().addOutboundPermitted(new PermittedOptions().setAddressRegex(".+"));
                options.addInboundPermitted(new PermittedOptions().setAddressRegex(".+"));
                router.route("/" + config.getString("event_bus") + "/*").handler(SockJSHandler.create(vertx).bridge(options, event -> {
                    event.complete(true);
                }));
            }
            router.route("/*").handler(ctx -> {
                app.call(ctx.request());
            });
        } catch (IOException e) {
            runtime.getErrorStream().println("Failed to create RackApplication");
        }

        vertx.createHttpServer(httpServerOptions).requestHandler(router::accept).listen();
    }

    @Override
    public void stop(Future<Void> future) throws Exception {
        this.runtime.tearDown();
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    private Ruby createRuntime(String root, JsonObject options) {
        Ruby runtime;
        RubyInstanceConfig instanceConfig = new RubyInstanceConfig();
        String jrubyHome = options.getString("jruby-home", "");
        if (!jrubyHome.isEmpty()) {
            instanceConfig.setJRubyHome(jrubyHome);
        }
        Object[] argv = options.getJsonArray("argv", new JsonArray()).getList().toArray();
        instanceConfig.setArgv(Arrays.copyOf(argv, argv.length, String[].class));
        RubyArray globalLoadPaths = (RubyArray) Ruby.getGlobalRuntime().getLoadService().getLoadPath();
        List<String> loadPaths = new ArrayList<>();
        for (int i = 0; i < globalLoadPaths.size(); i++) {
            IRubyObject entry = globalLoadPaths.eltInternal(i);
            loadPaths.add(entry.asJavaString());
        }
        instanceConfig.setLoadPaths(loadPaths);

        instanceConfig.setLoader(getClassLoader());
        runtime = Ruby.newInstance(instanceConfig);
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

    private ClassLoader getClassLoader() {
        if (this.classLoader != null) return this.classLoader;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) return cl;
        return getClass().getClassLoader();
    }

    private Ruby runtime;
    private ClassLoader classLoader;
}
