package org.jruby.jubilee;

import org.vertx.java.core.http.HttpServerResponse;

/**
 * Created with IntelliJ IDEA.
 * User: isaiah
 * Date: 11/29/12
 * Time: 5:39 PM
 */
public interface RackResponse {
    public void respond(HttpServerResponse response);
}
