package org.jruby.jubilee;

/**
 * Created with IntelliJ IDEA.
 * User: isaiah
 * Date: 11/26/12
 * Time: 12:01 PM
 */
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public interface RackErrors {
     /**
     * puts must be called with a single argument that responds to to_s.
     *
     * @param context ruby context
     * @param arg must respond to to_s
     * @return nil
     */
    IRubyObject puts( ThreadContext context, IRubyObject arg );

    /**
     * write must be called with a single argument that is a String.
     *
     * @param context ruby context
     * @param string a Ruby String
     * @return nil
     */
    IRubyObject write( ThreadContext context, IRubyObject string );

    /**
     * flush must be called without arguments and must be called in order to make the error appear for sure.
     *
     * @return nil
     */
    IRubyObject flush();

    /**
     * close must never be called on the error stream.
     *
     * @return nil
     */
    IRubyObject close();
}
