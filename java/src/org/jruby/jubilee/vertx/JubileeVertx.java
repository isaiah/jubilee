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
    public static Vertx vertx;
    private JubileeVertx() {
    }
    public static synchronized Vertx init(int port, String host) {
        if (vertx != null) return vertx;
        vertx = VertxFactory.newVertx(port, host);
        return vertx;
    }
    public static synchronized Vertx init(String host) {
        if (vertx != null) return vertx;
        vertx = VertxFactory.newVertx(host);
        return vertx;
    }
    public static synchronized Vertx init() {
        if (vertx != null) return vertx;
        vertx = VertxFactory.newVertx();
        return vertx;
    }

    public synchronized static Vertx vertx() {
        if (vertx == null) init();
        return vertx;
    }
}
