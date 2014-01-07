package org.jruby.jubilee.impl;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.jubilee.RackInput;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created with IntelliJ IDEA.
 * User: isaiah
 * Date: 11/26/12
 * Time: 12:11 PM
 */
public class RubyNullIO extends RubyObject implements RackInput {
    public static RubyClass createNullIOClass(Ruby ruby) {
        RubyModule jModule = ruby.getOrCreateModule("Jubilee");
        RubyClass nullIOClass = jModule.defineClassUnder("NullIO", ruby.getObject(), ALLOCATOR);
        nullIOClass.defineAnnotatedMethods(RubyNullIO.class);
        return nullIOClass;
    }

    public static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby ruby, RubyClass rubyClass) {
            return new RubyNullIO(ruby, rubyClass);
        }
    };

    public RubyNullIO(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    /**
     * gets must be called without arguments and return a string, or nil on EOF.
     *
     * @param context it's a JRuby thing
     * @return a string, or nil on EOF
     */
    @Override
    @JRubyMethod(name = "gets")
    public IRubyObject gets(ThreadContext context) {
        return getRuntime().getNil();
    }

    /**
     * read behaves like IO#read. Its signature is read([length, [buffer]]). If given,
     * length must be an non-negative Integer (>= 0) or nil, and buffer must be a
     * String and may not be nil. If length is given and not nil, then this method
     * reads at most length bytes from the input stream. If length is not given or
     * nil, then this method reads all data until EOF. When EOF is reached, this
     * method returns nil if length is given and not nil, or "" if length is not
     * given or is nil. If buffer is given, then the read data will be placed into
     * buffer instead of a newly created String object.
     *
     * @param context it's a JRuby thing
     * @param args    [length, [buffer]]
     * @return nil if length is given and not nil, or "" if length is not given or nil
     */
    @Override
    @JRubyMethod(optional = 2)
    public IRubyObject read(ThreadContext context, IRubyObject[] args) {
        return getRuntime().getNil();
    }

    /**
     * each must be called without arguments and only yield Strings.
     *
     * @param context it's a JRuby thing
     * @param block   that receives yield of Strings
     * @return pretty much nil
     */
    @Override
    @JRubyMethod
    public IRubyObject each(ThreadContext context, Block block) {
        return getRuntime().getNil();
    }

    /**
     * rewind must be called without arguments. It rewinds the input stream back
     * to the beginning. It must not raise Errno::ESPIPE: that is, it may not be
     * a pipe or a socket. Therefore, handler developers must buffer the input
     * data into some rewindable object if the underlying input stream is not rewindable.
     *
     * @param context it's a JRuby thing
     * @return pretty much nil
     */
    @Override
    @JRubyMethod
    public IRubyObject rewind(ThreadContext context) {
        return getRuntime().getNil();
    }

    /**
     * Close the input. Exposed only to the Java side because the Rack spec says
     * that application code must not call close, so we don't expose a close method to Ruby.
     */
    @Override
    public IRubyObject close(ThreadContext context) {
      return getRuntime().getNil();
    }
}
