package org.jruby.jubilee;

import org.jruby.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.jubilee.vertx.JubileeVertx;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.PlatformLocator;
import org.vertx.java.platform.PlatformManager;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by isaiah on 23/01/2014.
 */
public class RubyPlatformManager extends RubyObject {
    private PlatformManager pm;
    private RubyHash options;

    public static void createPlatformManagerClass(Ruby runtime) {
        RubyModule mJubilee = runtime.defineModule("Jubilee");
        RubyClass serverClass = mJubilee.defineClassUnder("PlatformManager", runtime.getObject(), ALLOCATOR);
        serverClass.defineAnnotatedMethods(RubyPlatformManager.class);
    }

    private static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby ruby, RubyClass rubyClass) {
            return new RubyPlatformManager(ruby, rubyClass);
        }
    };

    public RubyPlatformManager(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, IRubyObject config) {
        this.options = config.convertToHash();
        Ruby runtime = context.runtime;
        RubySymbol clustered_k = runtime.newSymbol("clustered");
        RubySymbol cluster_host_k = runtime.newSymbol("cluster_host");
        RubySymbol cluster_port_k = runtime.newSymbol("cluster_port");
        if (options.containsKey(clustered_k) && options.op_aref(context, clustered_k).isTrue()) {
            int clusterPort = 0;
            String clusterHost = null;
            if (options.containsKey(cluster_port_k))
                clusterPort = RubyNumeric.num2int(options.op_aref(context, cluster_port_k));
            if (options.containsKey(cluster_host_k))
                clusterHost = options.op_aref(context, cluster_host_k).asJavaString();
            if (clusterHost == null) clusterHost = getDefaultAddress();
            this.pm = PlatformLocator.factory.createPlatformManager(clusterPort, clusterHost);
        } else {
            this.pm = PlatformLocator.factory.createPlatformManager();
        }
        JubileeVertx.init(this.pm.vertx());
        return this;
    }
    @JRubyMethod(name = "start")
    public IRubyObject start(final ThreadContext context, final Block block) {
        final RubySymbol port_k = context.runtime.newSymbol("Port");
        JsonObject verticleConf =  new JsonObject(parseOptions(options));
        this.pm.deployVerticle("org.jruby.jubilee.JubileeVerticle", verticleConf,
                context.runtime.getJRubyClassLoader().getURLs(), 1, null, new AsyncResultHandler<String>() {
                    @Override
                    public void handle(AsyncResult<String> result) {
                        if (result.succeeded()) {
                            context.runtime.getOutputStream().println("yielding");
                            if (block.isGiven()) {
                                context.runtime.getOutputStream().println("yielding");
                                block.yieldSpecific(context);
                            }

                            context.runtime.getOutputStream().println("Jubilee is listening on port " + options.op_aref(context, port_k) + ", press Ctrl+C to quit");
                        } else {
                            result.cause().printStackTrace();
                        }
                    }
                });

        int ins = RubyNumeric.num2int(options.op_aref(context, RubySymbol.newSymbol(context.runtime, "instances"))) - 1;
        if (ins <= 0) return this;
        this.pm.deployVerticle("org.jruby.jubilee.JubileeVerticle", verticleConf,
                context.runtime.getJRubyClassLoader().getURLs(), ins, null, new AsyncResultHandler<String>() {
                    @Override
                    public void handle(AsyncResult<String> result) {
                        if (!result.succeeded()) result.cause().printStackTrace(context.runtime.getErrorStream());
                    }
                }
        );
        return this;
    }

    @JRubyMethod
    public IRubyObject stop(ThreadContext context) {
        this.pm.stop();
        return context.runtime.getNil();
    }

    private Map<String, Object> parseOptions(RubyHash options) {
        Ruby runtime = options.getRuntime();
        ThreadContext context = runtime.getCurrentContext();
        RubySymbol port_k = runtime.newSymbol("Port");
        RubySymbol host_k = runtime.newSymbol("Host");
        RubySymbol ssl_k = runtime.newSymbol("ssl");
        RubySymbol rack_app_k = runtime.newSymbol("rackapp");
        RubySymbol rack_up_k = runtime.newSymbol("rackup");
        RubySymbol ssl_keystore_k = runtime.newSymbol("ssl_keystore");
        RubySymbol ssl_password_k = runtime.newSymbol("ssl_password");
        RubySymbol eventbus_prefix_k = runtime.newSymbol("eventbus_prefix");
        RubySymbol quiet_k = runtime.newSymbol("quiet");
        RubySymbol environment_k = runtime.newSymbol("environment");
        RubySymbol root_k = runtime.newSymbol("root");
        Map<String, Object> map = new HashMap<>();
        map.put("host", options.op_aref(context, host_k).asJavaString());
        map.put("port", RubyNumeric.num2int(options.op_aref(context, port_k)));
        if (options.has_key_p(root_k).isTrue())
            map.put("root", options.op_aref(context, root_k).asJavaString());

        if (options.has_key_p(rack_up_k).isTrue())
            map.put("rackup", options.op_aref(context, rack_up_k).asJavaString());
        map.put("quiet", options.containsKey(quiet_k) && options.op_aref(context, quiet_k).isTrue());

        String environ = options.op_aref(context, environment_k).asJavaString();
        map.put("environment", environ);
        if (environ.equals("production"))
            map.put("hide_error_stack", true);

        boolean ssl = options.op_aref(context, ssl_k).isTrue();
        if (ssl) {
            map.put("keystore_path", options.op_aref(context, ssl_keystore_k).asJavaString());
            if (options.has_key_p(ssl_password_k).isTrue())
                map.put("keystore_password", options.op_aref(context, ssl_password_k).asJavaString());
        }
        map.put("ssl", ssl);
        if (options.has_key_p(eventbus_prefix_k).isTrue())
            map.put("event_bus", options.op_aref(context, eventbus_prefix_k).asJavaString());
        // This is a trick to put an Object into the config object
        // map.put("ruby", runtime);
        return map;
    }

    /*
    Get default interface to use since the user hasn't specified one
     */
    private String getDefaultAddress() {
        Enumeration<NetworkInterface> nets;
        try {
            nets = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            return null;
        }
        NetworkInterface netinf;
        while (nets.hasMoreElements()) {
            netinf = nets.nextElement();

            Enumeration<InetAddress> addresses = netinf.getInetAddresses();

            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();
                if (!address.isAnyLocalAddress() && !address.isMulticastAddress()
                        && !(address instanceof Inet6Address)) {
                    return address.getHostAddress();
                }
            }
        }
        return null;
    }
}
