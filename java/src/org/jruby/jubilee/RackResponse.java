package org.jruby.jubilee;

import org.jruby.runtime.builtin.IRubyObject;
import org.vertx.java.core.http.HttpServerResponse;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: isaiah
 * Date: 11/29/12
 * Time: 5:39 PM
 */
public interface RackResponse {
    public void respond(HttpServerResponse response);
}
