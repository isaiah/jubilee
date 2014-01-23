package org.jruby.jubilee;

import org.jruby.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.PlatformLocator;
import org.vertx.java.platform.PlatformManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by isaiah on 23/01/2014.
 */
public class RubyPlatformManager extends RubyObject {
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
    RubyHash options = config.convertToHash();
    PlatformManager pm = PlatformLocator.factory.createPlatformManager();
    pm.deployVerticle("org.jruby.jubilee.JubileeVerticle", new JsonObject(parseOptions(options)),
            context.runtime.getJRubyClassLoader().getURLs(), 10, null, new AsyncResultHandler<String>() {
      @Override
      public void handle(AsyncResult<String> result) {
        if (result.succeeded()) {
          System.out.println("Deployment ID is " + result.result());
        } else{
          result.cause().printStackTrace();
        }
      }
    });
    return this;
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
    Map<String, Object> map = new HashMap<>();
    map.put("host", options.op_aref(context, host_k).asJavaString());
    map.put("port", RubyNumeric.num2int(options.op_aref(context, port_k)));

    map.put("rackup", options.op_aref(context, rack_up_k).asJavaString());
    map.put("rackapp", options.op_aref(context, rack_app_k));
    map.put("quiet", options.op_aref(context, quiet_k).isTrue());

    map.put("environment", options.op_aref(context, environment_k).asJavaString());

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
    map.put("ruby", runtime);
    return map;
  }
}
