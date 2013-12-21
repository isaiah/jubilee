package org.jruby.jubilee;

import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by isaiah on 21/12/2013.
 */
public final class ApplicationMgr {
    private ThreadContext context;
    private IRubyObject configurator;
    private boolean ssl;
    private Queue<RackApplication> apps;

    public ApplicationMgr(ThreadContext context, IRubyObject configurator, boolean ssl, int poolSize) {
        this.context = context;
        this.configurator = configurator;
        this.ssl = ssl;
        this.apps = new ArrayBlockingQueue<RackApplication>(poolSize);
    }

    public RackApplication getApp() {
        RackApplication rackApp;
        rackApp = apps.poll();
        if (rackApp != null) return rackApp;
        context.runtime.getOutputStream().println("======================");
        context.runtime.getOutputStream().println("create new app");


        context.runtime.getOutputStream().println("======================");
        IRubyObject app = configurator.callMethod(context, "app");
        rackApp = new RackApplication(context, app, ssl, this);
        return rackApp;
    }

    public synchronized void returnApp(RackApplication app) {
        this.apps.offer(app);
    }
}
