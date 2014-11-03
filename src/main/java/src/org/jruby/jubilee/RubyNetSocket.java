package org.jruby.jubilee;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.vertx.core.Handler;
import io.vertx.core.VoidHandler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.streams.WriteStream;
import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class create a ruby IO interface by wrapping a Vertx NetSocket object.
 * <p/>
 * Not threadsafe.
 */
@JRubyClass(name="NetSocket")
public class RubyNetSocket extends RubyObject {
    private NetSocket sock;
    private ByteBuf buf;
    private AtomicBoolean eof;
    private boolean readClosed = false;
    private boolean writeClosed = false;
    private boolean closed = false;

    private final static int BUFSIZE = 4096 * 2;

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
        this.buf = Unpooled.buffer(BUFSIZE);
        this.eof = new AtomicBoolean(false);
        this.sock.dataHandler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer buffer) {
                if (buf.isWritable(buffer.length()))
                    buf.writeBytes(buffer.getByteBuf());
                else sock.pause();
            }
        });

        this.sock.endHandler(new VoidHandler() {
            @Override
            protected void handle() {
                eof.set(true);
            }
        });
    }

    /**
     * Both of the calls block
     *
     * @param context
     * @param args
     * @return
     */
    @JRubyMethod(name = {"read", "read_nonblock"}, required = 1, optional = 1)
    public IRubyObject read(ThreadContext context, IRubyObject[] args) {
        if (this.readClosed) throw context.runtime.newIOError("closed stream");
        int length = RubyNumeric.num2int(args[0]);
        byte[] data;
        if (args.length == 1)
            data = new byte[length];
        else data = ((RubyString) args[1]).getBytes();
        if (!(eof.get() || buf.isReadable())) {
            this.buf.clear();
            this.sock.resume();
        }
        waitReadable(this.buf);
        while (!eof.get() && length > 0) {
            int readedLength = Math.min(this.buf.readableBytes(), length);
            this.buf.readBytes(data, this.buf.readerIndex(), readedLength);
            length -= readedLength;
        }
        return context.runtime.newString(new ByteList(data));
    }

    /**
     * Though required by rack spec to impelement write_nonblock, it's just easier to block both of the calls.
     *
     * @param context the calling threadcontext
     * @param str     the string to write to the underline stream
     * @return the length written
     */
    @JRubyMethod(name = {"write", "write_nonblock"}, required = 1)
    public IRubyObject write(ThreadContext context, IRubyObject str) {
        if (this.writeClosed) throw context.runtime.newIOError("closed stream");
        RubyString data;
        if (str instanceof RubyString)
            data = (RubyString) str;
        else
            data = (RubyString) str.callMethod(context, "to_s");
        if (this.sock.writeQueueFull())
            waitWritable(this.sock);
        this.sock.writeString(data.asJavaString());
        // TODO return the length actually written
        return data.length();
    }

    @JRubyMethod(name = "close_write")
    public IRubyObject closeWrite(ThreadContext context) {
        this.writeClosed = true;
        return context.runtime.getTrue();
    }

    @JRubyMethod(name = "closeRead")
    public IRubyObject closeRead(ThreadContext context) {
        this.readClosed = true;
        return context.runtime.getTrue();
    }

    @JRubyMethod(name = "close")
    public IRubyObject close(ThreadContext context) {
        this.sock.close();
        this.closed = true;
        return context.runtime.getTrue();
    }

    @JRubyMethod(name = "closed?")
    public IRubyObject isClosed(ThreadContext context) {
        return context.runtime.newBoolean(this.closed);
    }

    @JRubyMethod(name = "flush")
    public IRubyObject flush(ThreadContext context) {
        return context.runtime.getTrue();
    }

    private void waitWritable(WriteStream<?> stream) {
        final AtomicBoolean writable = new AtomicBoolean(false);
        stream.drainHandler(new Handler<Void>() {
            @Override
            public void handle(Void empty) {
                writable.set(true);
            }
        });
        while (!writable.get())
            ;
    }

    private void waitReadable(ByteBuf buf) {
        while (!buf.isReadable())
            ;
    }
}
