package org.jruby.jubilee.impl;

import org.jruby.jubilee.RackErrors;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created with IntelliJ IDEA.
 * User: isaiah
 * Date: 11/26/12
 * Time: 12:03 PM
 */
public class RubyIORackErrors implements RackErrors {
    @Override
    public IRubyObject puts(ThreadContext context, IRubyObject arg) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public IRubyObject write(ThreadContext context, IRubyObject string) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public IRubyObject flush() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public IRubyObject close() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
