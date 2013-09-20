package org.jruby.jubilee.vertx;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.shareddata.SharedData;

/**
 * Created with IntelliJ IDEA.
 * User: isaiah
 * Date: 20/09/2013
 * Time: 15:24
 */
public class JubileeVertx {
    private static Vertx vertx;
    private JubileeVertx() {
    }

    public static synchronized Vertx vertx() {
        if (vertx == null) vertx = VertxFactory.newVertx();
        return vertx;
    }
}
