package org.jruby.jubilee;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jruby.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetSocket;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by isaiah on 1/7/14.
 */
public class RubyNetSocket extends RubyObject {
    private NetSocket sock;
    private ByteBuf buf;
    private AtomicBoolean end;

    public static RubyClass createNetSocketClass(final Ruby runtime) {
        RubyModule jubilee = runtime.getOrCreateModule("Jubilee");
        RubyClass klazz = jubilee.defineClassUnder("NetSocket", runtime.getObject(), new ObjectAllocator() {
            @Override
            public IRubyObject allocate(Ruby ruby, RubyClass rubyClass) {
                return new RubyNetSocket(ruby, rubyClass);
            }
        });
        klazz.defineAnnotatedMethods(RubyNetSocket.class);
        return klazz;
    }

    public RubyNetSocket(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }

    public RubyNetSocket(Ruby ruby, RubyClass rubyClass, NetSocket socket) {
        super(ruby, rubyClass);
        this.sock = socket;
        this.buf = Unpooled.buffer();
        this.end = new AtomicBoolean(false);
        this.sock.dataHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                buf.writeBytes(buffer.getByteBuf());
            }
        });

        this.sock.endHandler(new VoidHandler() {
            @Override
            protected void handle() {
                end.set(true);
            }
        });
    }

    @JRubyMethod(name = {"read", "read_nonblock"}, required = 1, optional = 1)
    public IRubyObject read(ThreadContext context, IRubyObject[] args) {
        int length = RubyNumeric.num2int(args[0]);
        byte[] data;
        if (args.length == 1)
            data = new byte[length];
        else data = ((RubyString) args[1]).getBytes();
        this.buf.readBytes(data, this.buf.readerIndex(), length);
        return RubyString.newString(context.runtime, data.toString());
    }

    @JRubyMethod(name = {"write", "write_nonblock"}, required = 1)
    public IRubyObject write(ThreadContext context, IRubyObject str) {
        RubyString data;
        if (str instanceof RubyString)
            data = (RubyString) str;
        else
            data = (RubyString) str.callMethod(context, "to_s");
        this.sock.write(data.asJavaString());
        return data.length();
    }

    @JRubyMethod(name = "close_write")
    public IRubyObject closeWrite(ThreadContext context) {
        return context.runtime.getTrue();
    }
    @JRubyMethod(name = "closeRead")
    public IRubyObject closeRead(ThreadContext context) {
        return context.runtime.getTrue();
    }
    @JRubyMethod(name = "close")
    public IRubyObject close(ThreadContext context) {
        return context.runtime.getTrue();
    }
    @JRubyMethod(name = "closed?")
    public IRubyObject isClosed(ThreadContext context) {
        return context.runtime.getTrue();
    }
    @JRubyMethod(name = "flush")
    public IRubyObject flush(ThreadContext context) {
        return context.runtime.getTrue();
    }
}
