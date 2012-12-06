package org.jruby.jubilee.deploy;

import org.jruby.jubilee.Server;

import java.util.concurrent.CountDownLatch;

/**
 * Created with IntelliJ IDEA.
 * User: isaiah
 * Date: 12/3/12
 * Time: 7:37 PM
 */
public class Starter {
  private CountDownLatch stopLatch = new CountDownLatch(1);

  public void block() {
    while(true) {
      try {
        stopLatch.await();
        break;
      } catch (InterruptedException ignore) {}
    }
  }

  public void unblock() {
    stopLatch.countDown();
  }
}
