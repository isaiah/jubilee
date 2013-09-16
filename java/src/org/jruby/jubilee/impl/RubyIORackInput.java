package org.jruby.jubilee.impl;

import io.netty.buffer.ByteBuf;
import org.jcodings.specific.ASCIIEncoding;
import org.jcodings.Encoding;
import org.jruby.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.jubilee.Const;
import org.jruby.jubilee.RackInput;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.vertx.java.core.http.HttpServerRequest;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created with IntelliJ IDEA.
 * User: isaiah
 * Date: 11/26/12
 * Time: 10:12 PM
 */
public class RubyIORackInput extends RubyObject implements RackInput {
    private Encoding BINARY = ASCIIEncoding.INSTANCE;
    private HttpServerRequest request;
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

    public static RubyClass createRubyIORackInputClass(Ruby runtime) {
        RubyModule jModule = runtime.defineModule("Jubilee");
        RubyClass rackIOInputClass = jModule.defineClassUnder("RubyIORackInput", runtime.getObject(), ALLOCATOR);
        rackIOInputClass.defineAnnotatedMethods(RubyIORackInput.class);
        return rackIOInputClass;
    }

    public RubyIORackInput(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public RubyIORackInput(Ruby runtime, HttpServerRequest request, ByteBuf buf, AtomicBoolean eof) {
        this(runtime, createRubyIORackInputClass(runtime));
        this.request = request;
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
      RubyString line = RubyString.newEmptyString(getRuntime(), BINARY);
        if (isEOF())
            return getRuntime().getNil();
        int lineEnd = -1;
        while (lineEnd == -1 && !isEOF())
            lineEnd = buf.indexOf(buf.readerIndex(), buf.writerIndex(), Const.EOL);

        // No line break found, read all
        if (lineEnd == -1)
            return readAll(line);

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
        RubyString dst = RubyString.newEmptyString(getRuntime(), BINARY);
        if (isEOF())
            return getRuntime().getNil();
        int length;
        switch (args.length) {
            case 0:
                return readAll(dst);
            case 1:
                length = RubyInteger.num2int(args[0]);
                break;
            default:
                length = RubyInteger.num2int(args[0]);
                dst = (RubyString) args[1];
        }
        if (length < 0)
            getRuntime().newArgumentError("Negative length " + length + " given");
        byte[] buffer = new byte[length];
        int toRead = length;
        while (toRead > 0 && !isEOF()) {
            int len = Math.min(toRead, readableBytes());
            buf.readBytes(buffer, length - toRead, len);
            toRead = toRead - len;
        }
        if (toRead > 0)
            buffer = Arrays.copyOfRange(buffer, 0, length - toRead);
        return dst.cat(buffer);
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
        return getRuntime().getNil();
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
        return getRuntime().getNil();
    }

    /**
     * Close the input. Exposed only to the Java side because the Rack spec says
     * that application code must not call close, so we don't expose a close method to Ruby.
     */
    @JRubyMethod
    public IRubyObject close(ThreadContext context) {
        buf.clear();
        return getRuntime().getNil();
    }

    private int readableBytes() {
      if (! this.chunked) {
        return Math.min(buf.readableBytes(), this.len - buf.readerIndex());
      }
      return buf.readableBytes();
    }

    private boolean isEOF() {
        while (buf.readableBytes() == 0 && !eof.get())
            ; // wait while there is nothing to read
        return buf.readableBytes() == 0 && eof.get();
    }

    private IRubyObject readAll(RubyString dst) {
        buf.readerIndex(0);
        while(!eof.get())
            ; // wait until all data received
        int length = this.chunked ? buf.readableBytes() : Math.min(this.len, buf.readableBytes());
        byte[] data = new byte[length];
        dst.cat(data);
        buf.readBytes(data);
        return dst.isEmpty() ? getRuntime().getNil() : dst;
    }
}
