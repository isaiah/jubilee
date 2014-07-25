package org.jruby.jubilee;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import org.jruby.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.jubilee.vertx.JubileeVertx;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by isaiah on 23/01/2014.
 */
public class RubyPlatformManager extends RubyObject {
    private RubyHash options;
    private Vertx vertx;

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

        VertxOptions vertxOptions = new VertxOptions();
        if (options.containsKey(clustered_k) && options.op_aref(context, clustered_k).isTrue()) {
            int clusterPort = 0;
            String clusterHost = null;
            if (options.containsKey(cluster_port_k))
                clusterPort = RubyNumeric.num2int(options.op_aref(context, cluster_port_k));
            if (options.containsKey(cluster_host_k))
                clusterHost = options.op_aref(context, cluster_host_k).asJavaString();
            if (clusterHost == null) clusterHost = getDefaultAddress();
            vertxOptions.setClustered(true).setClusterHost(clusterHost).setClusterPort(clusterPort);

        }

        this.vertx = Vertx.vertx(vertxOptions);
        JubileeVertx.init(this.vertx);
        return this;
    }

    @JRubyMethod(name = "start")
    public IRubyObject start(final ThreadContext context, final Block block) {

        DeploymentOptions verticleConf = parseOptions(options);
        this.vertx.deployVerticle("java:org.jruby.jubilee.JubileeVerticle", verticleConf, new AsyncResultHandler<String>() {
            @Override
            public void handle(AsyncResult<String> result) {
                if (result.succeeded()) {
                    if (block.isGiven()) {
                        block.yieldSpecific(context);
                    }
                } else {
                    result.cause().printStackTrace(context.runtime.getErrorStream());
                }
            }
        });
        return this;

    }

    @JRubyMethod
    public IRubyObject stop(ThreadContext context) {
        this.vertx.close();
        return context.runtime.getNil();
    }

    private DeploymentOptions parseOptions(RubyHash options) {
        Ruby runtime = options.getRuntime();
        ThreadContext context = runtime.getCurrentContext();
        RubySymbol port_k = runtime.newSymbol("Port");
        RubySymbol host_k = runtime.newSymbol("Host");
        RubySymbol ssl_k = runtime.newSymbol("ssl");
        RubySymbol rack_up_k = runtime.newSymbol("rackup");
        RubySymbol ssl_keystore_k = runtime.newSymbol("ssl_keystore");
        RubySymbol ssl_password_k = runtime.newSymbol("ssl_password");
        RubySymbol eventbus_prefix_k = runtime.newSymbol("eventbus_prefix");
        RubySymbol quiet_k = runtime.newSymbol("quiet");
        RubySymbol environment_k = runtime.newSymbol("environment");
        RubySymbol root_k = runtime.newSymbol("root");
        RubySymbol instances_k = RubySymbol.newSymbol(context.runtime, "instances");
        JsonObject map = new JsonObject();
        map.putString("host", options.op_aref(context, host_k).asJavaString());
        map.putNumber("port", RubyNumeric.num2int(options.op_aref(context, port_k)));

        map.putNumber("instances", RubyNumeric.num2int(options.op_aref(context, instances_k)));
        if (options.has_key_p(root_k).isTrue())
            map.putString("root", options.op_aref(context, root_k).asJavaString());

        map.putString("rackup", options.op_aref(context, rack_up_k).asJavaString());
        map.putBoolean("quiet", options.containsKey(quiet_k) && options.op_aref(context, quiet_k).isTrue());

        String environ = options.op_aref(context, environment_k).asJavaString();
        map.putString("environment", environ);
        if (environ.equals("production"))
            map.putBoolean("hide_error_stack", true);

        boolean ssl = options.op_aref(context, ssl_k).isTrue();
        if (ssl) {
            map.putString("keystore_path", options.op_aref(context, ssl_keystore_k).asJavaString());
            if (options.has_key_p(ssl_password_k).isTrue())
                map.putString("keystore_password", options.op_aref(context, ssl_password_k).asJavaString());
        }
        map.putBoolean("ssl", ssl);
        if (options.has_key_p(eventbus_prefix_k).isTrue())
            map.putString("event_bus", options.op_aref(context, eventbus_prefix_k).asJavaString());
        DeploymentOptions opts = new DeploymentOptions();
        opts.setConfig(map);
        return opts;
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
