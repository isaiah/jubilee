package org.jruby.jubilee;

import org.jruby.Ruby;
import org.jruby.jubilee.impl.DefaultRackEnvironment;
import org.jruby.jubilee.impl.RubyIORackInput;
import org.jruby.runtime.builtin.IRubyObject;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

/**
 * Created with IntelliJ IDEA.
 * User: isaiah
 * Date: 11/29/12
 * Time: 5:39 PM
 */
public class RackRequest {
    private RackEnvironment env;
    private Ruby runtime;

    public RackRequest(Ruby runtime, HttpServerRequest request) {
        this.runtime = runtime;

        final Buffer body = new Buffer(0);
        request.dataHandler(new Handler<Buffer>() {
            public void handle(Buffer buffer) {
                body.appendBuffer(buffer);
            }
        });
        RackInput input = new RubyIORackInput(runtime, body);
        env = new DefaultRackEnvironment(runtime, request, input);
    }

    public RackEnvironment getEnv() {
        return env;
    }

    public IRubyObject getRackEnv() {
        return env.getEnv();
    }
}
