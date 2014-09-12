package org.jruby.jubilee;

import io.netty.channel.Channel;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created by isaiah on 25/12/2013.
 */
@JRubyClass(name = "Channel")
public class RubyChannel extends RubyObject {
    private Channel chan;

    public static RubyClass createChannelClass(final Ruby runtime) {
        RubyModule vertxModule = runtime.getOrCreateModule("Jubilee");
        RubyClass klazz = vertxModule.defineClassUnder("Channel", runtime.getObject(), new ObjectAllocator() {
            @Override
            public IRubyObject allocate(Ruby ruby, RubyClass rubyClass) {
                return new RubyChannel(ruby, rubyClass);
            }
        });
        klazz.defineAnnotatedMethods(RubyChannel.class);
        return klazz;
    }

    public RubyChannel(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }

    @JRubyMethod
    public IRubyObject read(ThreadContext context) throws InterruptedException {
        return context.runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject write(ThreadContext context) {
        return context.runtime.getNil();

    }

    @JRubyMethod
    public IRubyObject readNonBlock(ThreadContext context) {

        return context.runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject writeNonBlock(ThreadContext context) {

        return context.runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject flush(ThreadContext context) {

        return context.runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject close(ThreadContext context) {

        return context.runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject closeRead(ThreadContext context) {

        return context.runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject closeWrite(ThreadContext context) {

        return context.runtime.getNil();
    }

    @JRubyMethod(name = "closed?")
    public IRubyObject isClosed(ThreadContext context) {

        return context.runtime.getNil();
    }
}
