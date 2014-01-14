package org.jruby.jubilee.vertx;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;

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
    if (JubileeVertx.vertx != null) return vertx;
    JubileeVertx.vertx = VertxFactory.newVertx(port, host);
    return JubileeVertx.vertx;
  }
  public static synchronized Vertx init(String host) {
    if (JubileeVertx.vertx != null) return vertx;
    JubileeVertx.vertx = VertxFactory.newVertx(host);
    return JubileeVertx.vertx;
  }
  public static synchronized Vertx init() {
    if (JubileeVertx.vertx != null) return vertx;
    JubileeVertx.vertx = VertxFactory.newVertx();
    return JubileeVertx.vertx;
  }

  public synchronized static Vertx vertx() {
    if (JubileeVertx.vertx == null) init();
    return JubileeVertx.vertx;
  }
}
