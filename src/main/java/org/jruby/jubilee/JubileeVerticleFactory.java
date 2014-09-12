/*
 * Copyright 2011-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jruby.jubilee;

import org.jruby.*;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.InvokeFailedException;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.builtin.IRubyObject;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxException;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Container;
import org.vertx.java.platform.Verticle;
import org.vertx.java.platform.VerticleFactory;
import org.vertx.java.platform.impl.WrappedVertx;

import java.io.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class JubileeVerticleFactory implements VerticleFactory {

    private ClassLoader cl;
    private ScriptingContainer scontainer;
    private static final AtomicInteger seq = new AtomicInteger();

    public static Vertx vertx;
    public static Container container;

    public JubileeVerticleFactory() {
    }

    @Override
    public void init(Vertx vertx, Container container, ClassLoader cl) {
        this.cl = cl;
        // These statics are used by the Rhino scripts to look up references to vertx and the container
        JubileeVerticleFactory.vertx = vertx;
        JubileeVerticleFactory.container = container;
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cl);
            this.scontainer = new ScriptingContainer(LocalContextScope.SINGLETHREAD);
            scontainer.setCompatVersion(CompatVersion.RUBY1_9);
            //Prevent JRuby from logging errors to stderr - we want to log ourselves
            scontainer.setErrorWriter(new NullWriter());
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    @Override
    public Verticle createVerticle(String rackup) throws Exception {
        return new RackVerticle(rackup);
    }

    public void reportException(Logger logger, Throwable t) {
        RaiseException je = null;
        if (t instanceof EvalFailedException) {
            EvalFailedException e = (EvalFailedException)t;
            Throwable cause = e.getCause();
            if (cause instanceof RaiseException) {
                je = (RaiseException)cause;
            }
        } else if (t instanceof RaiseException) {
            je = (RaiseException)t;
        }

        if (je != null) {

            RubyException re = je.getException();

            String msg;
            if (re instanceof RubyNameError) {
                RubyNameError rne = (RubyNameError)re;
                msg = "Invalid or undefined name: " + rne.name().toString();
            } else {
                msg = re.message.toString();
            }

            StringBuilder backtrace = new StringBuilder();
            IRubyObject bt = re.backtrace();

            if (bt instanceof List) {
                for (Object obj : (List<?>)bt) {
                    if (obj instanceof String) {
                        String line = (String)obj;
                        addToBackTrace(backtrace, line);
                    }
                }
            }
            logger.error("Exception in Ruby verticle: " + msg +
                    "\n" + backtrace);
        } else {
            logger.error("Unexpected exception in Ruby verticle", t);
        }
    }

    private void addToBackTrace(StringBuilder backtrace, String line) {
        if (line.contains(".rb")) {
            //We filter out any Java stack trace
            backtrace.append(line).append('\n');
        }
    }

    // This method synchronizes the callback into the JRuby code to make sure we don't have concurrent requires
    // or loads occurring in the same JRuby container
    public static synchronized void requireCallback(Runnable runnable) {
        runnable.run();
    }

    // This MUST be static or we will get a leak since JRuby maintains it and a non static will hold a reference
    // to this
    private static class NullWriter extends Writer {

        public void write(char[] cbuf, int off, int len) throws IOException {
        }

        public void flush() throws IOException {
        }

        public void close() throws IOException {
        }
    }

    public void close() {
        scontainer.clear();
        // JRuby keeps a static reference to a runtime - we must clear this manually to avoid a leak
        Ruby.clearGlobalRuntime();
    }
    private class RackVerticle extends Verticle {
        public RackVerticle(String rackup) {
            this.rackup = rackup;
            this.vertx = JubileeVerticleFactory.vertx;
            this.container = JubileeVerticleFactory.container;
        }

        public void start() {
            JsonObject config = container.config();
            synchronized (JubileeVerticleFactory.class) {
                try (InputStream is = cl.getResourceAsStream(rackup)) {
                    if (is == null) {
                        throw new IllegalArgumentException("Cannot find verticle: " + rackup);
                    }
                    modName = "Mod___VertxInternalVert__" + seq.incrementAndGet();
                    StringBuilder svert = new StringBuilder( "require 'core/vertx_require'\n").append("module ").append(modName).append(";extend self;");
                    String rackScript = "require 'rack'\n" +
                            "require 'jubilee'\n" +
                            "app, _ = Rack::Builder.parse_file('" + rackup + "')\n";
                    if (!config.getBoolean("quiet", true) && config.getString("environment").equals("development")) {
                        rackScript += "logger = STDOUT\n" +
                                "app = Rack::CommonLogger.new(app, logger)\n";
                    }
                    rackScript += "Jubilee::Application.new(app)\n";
                    svert.append(rackScript);
                    svert.append("end");
                    rackApplication = (IRubyObject)scontainer.runScriptlet(new StringReader(svert.toString()), rackup);
                } catch (Exception e) {
                    throw new VertxException(e);
                }
            }
            httpServer = vertx.createHttpServer();

            final RackApplication app;
            boolean ssl = config.getBoolean("ssl", false);
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
                httpServer.listen(config.getInteger("port", 8080), config.getString("host", "localhost"));
            } catch (IOException e) {
                container.logger().fatal("Failed to create RackApplication");
            }
        }

        @Override
        public void stop() {
            if (this.httpServer != null)
                this.httpServer.close();
            if (rackApplication != null) {
                try {
                    // We call the script with receiver = null - this causes the method to be called on the top level
                    // script
                    scontainer.callMethod(rackApplication, "vertx_stop");
                } catch (InvokeFailedException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RaiseException) {
                        // Gosh, this is a bit long winded!
                        RaiseException re = (RaiseException) cause;
                        String msg = "(NoMethodError) undefined method `vertx_stop'";
                        if (re.getMessage().startsWith(msg)) {
                            // OK - method is not mandatory
                            return;
                        }
                    }
                    throw e;
                } finally {
                    // Remove the module const
                    scontainer.runScriptlet("Object.send(:remove_const, :" + modName + ")");
                }
            }
        }

        private String rackup;
        private IRubyObject rackApplication;
        private String modName;
        private HttpServer httpServer;
    }
}
