package org.jruby.jubilee.vertx;

import io.vertx.core.Vertx;

/**
 * Created with IntelliJ IDEA.
 * User: isaiah
 * Date: 20/09/2013
 * Time: 15:24
 */
public class JubileeVertx {
    public static Vertx vertx;

    private JubileeVertx() {
    }

    public static void init(Vertx vertx) {
        JubileeVertx.vertx = vertx;
    }

    public synchronized static Vertx vertx() {
        if (JubileeVertx.vertx == null)
            throw new RuntimeException("vertx is not initialized, do you run in jubilee server?");
        return JubileeVertx.vertx;
    }
}
