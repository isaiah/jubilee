package org.jruby.jubilee;

import io.vertx.core.*;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.spi.cluster.VertxSPI;
import io.vertx.ext.sockjs.BridgeOptions;
import io.vertx.ext.sockjs.SockJSServer;
import io.vertx.ext.sockjs.SockJSServerOptions;
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
    public void start(Future<Void> voidFuture) throws Exception{
        JsonObject config = getConfig();
        HttpServerOptions httpServerOptions = new HttpServerOptions();
        httpServerOptions.setPort(config.getInteger("port"))
                .setAcceptBacklog(10000)
                .setHost(config.getString("host"));

        String root = config.getString("root", ".");
        this.runtime = createRuntime(root, config);
        String expandedRoot = this.runtime.evalScriptlet("File.expand_path(%q(" + root + "))").asJavaString();
        this.runtime.setCurrentDirectory(expandedRoot);
        IRubyObject rackApplication = initRackApplication(config);
        final RackApplication app;
        boolean ssl = config.getBoolean("ssl");
        if (ssl) {
            KeyCertOptions keyStoreOptions = new KeyCertOptions();
            keyStoreOptions.setCertPath(config.getString("keystore_path"))
                    .setKeyPath((config.getString("keystore_password")));
            httpServerOptions.setSsl(true).setKeyStore(keyStoreOptions);
        }
        HttpServer httpServer = getVertx().createHttpServer(httpServerOptions);
        try {
            app = new RackApplication((VertxSPI) getVertx(), runtime.getCurrentContext(), rackApplication, config);
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
                SockJSServerOptions sockjsOptions = new SockJSServerOptions();
                sockjsOptions.setPrefix(config.getString("event_bus"));
                SockJSServer sockJSServer = SockJSServer.newSockJSServer(getVertx(), httpServer);
                sockJSServer.bridge(sockjsOptions, new BridgeOptions());
            }

        } catch (IOException e) {
            runtime.getErrorStream().println("Failed to create RackApplication");
        }
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
//        if (Ruby.isGlobalRuntimeReady()) {
//            runtime = Ruby.getGlobalRuntime();
//        } else {
        RubyInstanceConfig instanceConfig = new RubyInstanceConfig();
        String jrubyHome = options.getString("jruby-home", "");
        if (!jrubyHome.isEmpty()) {
            instanceConfig.setJRubyHome(jrubyHome);
        }
        Object[] argv = options.getArray("argv", new JsonArray(new String[]{})).toArray();
        instanceConfig.setArgv(Arrays.copyOf(argv, argv.length, String[].class));
//        }
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
