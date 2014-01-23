package org.jruby.jubilee;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.PlatformLocator;
import org.vertx.java.platform.PlatformManager;

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
  public IRubyObject initialize(ThreadContext context) {
    PlatformManager pm = PlatformLocator.factory.createPlatformManager();
    JsonObject conf = new JsonObject().putString("host", "0.0.0.0").putNumber("port", 8080).putString("rackup", "config.ru");
    pm.deployVerticle("org.jruby.jubilee.JubileeVerticle", conf, context.runtime.getJRubyClassLoader().getURLs(), 10, null, new AsyncResultHandler<String>() {

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
}
