package org.jruby.jubilee.impl;

import io.netty.buffer.ByteBuf;
import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.jubilee.Const;
import org.jruby.jubilee.RackInput;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

import java.util.concurrent.atomic.AtomicBoolean;

import io.vertx.core.http.HttpServerRequest;

/**
 * Created with IntelliJ IDEA.
 * User: isaiah
 * Date: 11/26/12
 * Time: 10:12 PM
 */
public class RubyIORackInput extends RubyObject implements RackInput {
    private Encoding BINARY = ASCIIEncoding.INSTANCE;
    private int len;
    private boolean chunked;
    private ByteBuf buf;
    private AtomicBoolean eof;

    public static ObjectAllocator ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby ruby, RubyClass rubyClass) {
            return new RubyIORackInput(ruby, rubyClass);
        }
    };

    public static RubyClass createIORackInputClass(Ruby runtime) {
        RubyModule jModule = runtime.getOrCreateModule("Jubilee");
        RubyClass rackIOInputClass = jModule.defineClassUnder("IORackInput", runtime.getObject(), ALLOCATOR);
        rackIOInputClass.defineAnnotatedMethods(RubyIORackInput.class);
        return rackIOInputClass;
    }

    public RubyIORackInput(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public RubyIORackInput(Ruby runtime, RubyClass rubyClass, HttpServerRequest request, ByteBuf buf, AtomicBoolean eof) {
        this(runtime, rubyClass);
        String hdr = request.headers().get(Const.Vertx.CONTENT_LENGTH);
        this.chunked = hdr == null;
        this.len = this.chunked ? 0 : Integer.parseInt(hdr);
        this.buf = buf;
        this.eof = eof;
    }

    /**
     * gets must be called without arguments and return a string, or nil on EOF.
     * <p/>
     * this method return one line a time.
     *
     * @param context it's a JRuby thing
     * @return a string, or nil on EOF
     */
    @Override
    @JRubyMethod
    public IRubyObject gets(ThreadContext context) {
        Ruby runtime = context.runtime;
        RubyString line = RubyString.newEmptyString(context.runtime, BINARY);

        if (isEOF()) return runtime.getNil();

        int lineEnd = buf.indexOf(buf.readerIndex(), buf.writerIndex(), Const.EOL);
        while (lineEnd == -1 && !eof.get()) {
            lineEnd = buf.indexOf(buf.readerIndex(), buf.writerIndex(), Const.EOL);
        }

        // No line break found, read all
        if (lineEnd == -1) return readAll(runtime, line);

        int readLength = lineEnd - buf.readerIndex();
        byte[] dst = new byte[readLength + 1];
        buf.readBytes(dst);
        return line.cat(dst);
    }

    /**
     * read behaves like IO#read. Its signature is read([length, [buffer]]). If given,
     * length must be an non-negative Integer (>= 0) or nil, and buffer must be a
     * String and may not be nil. If length is given and not nil, then this method
     * reads at most length bytes from the input stream. If length is not given or
     * nil, then this method reads all data until EOF. When EOF is reached, this
     * method returns nil if length is given and not nil, or "" if) length is not
     * given or is nil. If buffer is given, then the read data will be placed into
     * buffer instead of a newly created String object.
     *
     * @param context it's a JRuby thing
     * @param args    [length, [buffer]]
     * @return nil if length is given and not nil, or "" if length is not given or nil
     */
    @Override
    @JRubyMethod(optional = 2)
    public IRubyObject read(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        RubyString dst = RubyString.newStringNoCopy(runtime, new ByteList(), BINARY, StringSupport.CR_VALID);
        if (isEOF())
            return RubyString.newEmptyString(runtime);
        int length;
        switch (args.length) {
            case 0:
                return readAll(runtime, dst);
            case 1:
                length = RubyNumeric.num2int(args[0]);
                break;
            default:
                length = RubyNumeric.num2int(args[0]);
                dst = (RubyString) args[1];
        }
        if (length < 0)
            runtime.newArgumentError("Negative length " + length + " given");
        byte[] buffer = new byte[length];
        int toRead = length;
        while (toRead > 0 && !isEOF()) {
            int len = Math.min(toRead, readableBytes());
            buf.readBytes(buffer, length - toRead, len);
            toRead = toRead - len;
        }
        if (toRead > 0) length -= toRead;
        return dst.cat(buffer, 0, length);
    }

    /**
     * each must be called without arguments and only yield Strings.
     *
     * @param context it's a JRuby thing
     * @param block   that receives yield of Strings
     * @return pretty much nil
     */
    @Override
    @JRubyMethod
    public IRubyObject each(ThreadContext context, Block block) {
        IRubyObject str;
        while (!(str = gets(context)).isNil()) {
            block.yield(context, str);
        }
        return context.runtime.getNil();
    }

    /**
     * rewind must be called without arguments. It rewinds the input stream back
     * to the beginning. It must not raise Errno::ESPIPE: that is, it may not be
     * a pipe or a socket. Therefore, handler developers must buffer the input
     * data into some rewindable object if the underlying input stream is not rewindable.
     *
     * @param context it's a JRuby thing
     * @return pretty much nil
     */
    @Override
    @JRubyMethod
    public IRubyObject rewind(ThreadContext context) {
        buf.readerIndex(0);
        return context.runtime.getNil();
    }

    /**
     * Close the input. Exposed only to the Java side because the Rack spec says
     * that application code must not call close, so we don't expose a close method to Ruby.
     */
    @JRubyMethod
    public IRubyObject close(ThreadContext context) {
        buf.clear();
        return context.runtime.getNil();
    }

    private int readableBytes() {
        if (!this.chunked) {
            return Math.min(buf.readableBytes(), this.len - buf.readerIndex());
        }
        return buf.readableBytes();
    }

    private boolean isEOF() {
        while (buf.readableBytes() == 0 && !eof.get())
            ; // wait while there is nothing to read
        return buf.readableBytes() == 0 && eof.get();
    }

    private IRubyObject readAll(Ruby runtime, RubyString dst) {
        while (!eof.get())
            ; // wait until all data received
        int length = this.chunked ? buf.readableBytes() : Math.min(this.len, buf.readableBytes());
        byte[] data = new byte[length];
        buf.readBytes(data);
        dst.cat(data);
        return dst.isEmpty() ? runtime.getNil() : dst;
    }
}
