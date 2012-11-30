package org.jruby.jubilee;

import org.jruby.Ruby;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Created with IntelliJ IDEA.
 * User: isaiah
 * Date: 11/29/12
 * Time: 5:40 PM
 */
public class RackApplication {
    private IRubyObject app;

    public RackApplication(IRubyObject app) {
        this.app = app;
    }

    public RackResponse call(RackRequest request) {
        IRubyObject result = app.callMethod(getRuntime().getCurrentContext(), "call", request.getRackEnv());
        return (RackResponse) JavaEmbedUtils.rubyToJava(getRuntime(), result, RackResponse.class);
    }

    public Ruby getRuntime() {
        return app.getRuntime();
    }
}
