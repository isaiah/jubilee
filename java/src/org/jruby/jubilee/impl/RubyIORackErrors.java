package org.jruby.jubilee.impl;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.jubilee.RackErrors;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created with IntelliJ IDEA.
 * User: isaiah
 * Date: 11/26/12
 * Time: 12:03 PM
 */
public class RubyIORackErrors extends RubyObject implements RackErrors {

    public static RubyClass createRubyIORackErrorsClass(Ruby runtime) {
        RubyModule jModule = runtime.defineModule("Jubilee");
        RubyClass rackErrorsClass = jModule.defineClassUnder("RubyIORackErrors", runtime.getObject(), ALLOCATOR);
        rackErrorsClass.defineAnnotatedMethods(RubyIORackErrors.class);
        return rackErrorsClass;
    }

    public static final ObjectAllocator ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby ruby, RubyClass rubyClass) {
            return new RubyIORackErrors(ruby, rubyClass);
        }
    };

    public RubyIORackErrors(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public RubyIORackErrors(Ruby runtime) {
        super(runtime, createRubyIORackErrorsClass(runtime));
    }

    @Override
    @JRubyMethod(name = "puts")
    public IRubyObject puts(ThreadContext context, IRubyObject arg) {
        //getRuntime().getOutputStream().println(arg.toString());
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    @JRubyMethod
    public IRubyObject write(ThreadContext context, IRubyObject string) {
        //getRuntime().getOutputStream().println(string.toString());
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    @JRubyMethod
    public IRubyObject flush() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    @JRubyMethod
    public IRubyObject close() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
