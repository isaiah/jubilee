package org.jruby.jubilee;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A RubyClass that expose a call method, like a proc
 * There must be a class in JRuby for this, but I just couldn't find it.
 */
public class RubyCallable extends RubyObject {
  private Callable callable;

  public static RubyClass createClallableClass(final Ruby runtime) {
    RubyModule jubilee = runtime.getOrCreateModule("Jubilee");
    RubyClass klazz = jubilee.defineClassUnder("Callable", runtime.getObject(), new ObjectAllocator() {
      @Override
      public IRubyObject allocate(Ruby ruby, RubyClass rubyClass) {
        return new RubyCallable(ruby, rubyClass);
      }
    });
    klazz.defineAnnotatedMethods(RubyCallable.class);
    return klazz;
  }

  public RubyCallable(Ruby ruby, RubyClass rubyClass) {
    super(ruby, rubyClass);
  }

  public RubyCallable(Ruby ruby, RubyClass rubyClass, Callable callable) {
    super(ruby, rubyClass);
    this.callable = callable;
  }

  @JRubyMethod
  public IRubyObject call(ThreadContext context) {
    this.callable.call();
    return context.runtime.getNil();
  }

  public static abstract class Callable {
    public void call(){};
  }
}
