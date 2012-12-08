package org.jruby.jubilee.impl;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jruby.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.jubilee.Const;
import org.jruby.jubilee.RackInput;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.vertx.java.core.buffer.Buffer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: isaiah
 * Date: 11/26/12
 * Time: 10:12 PM
 */
public class RubyIORackInput extends RubyObject implements RackInput {
  private ChannelBuffer buf;
  private CountDownLatch bodyLatch;

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

  public RubyIORackInput(Ruby runtime, Buffer buf, CountDownLatch bodyLatch) {
    this(runtime, createRubyIORackInputClass(runtime));
    this.buf = buf.getChannelBuffer();
    this.bodyLatch = bodyLatch;
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
    // TODO this is not the optimistic way. remove the latch and implement tee_input as unicorn
    try {
      bodyLatch.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException ignore) {
      return getRuntime().getNil();
    }
    if (buf.readableBytes() == 0)
      return getRuntime().getNil();
    int lineEnd = buf.indexOf(buf.readerIndex(), buf.capacity(), Const.EOL);
    int readLength;
    if ((readLength = lineEnd - buf.readerIndex()) > 0) {
      byte[] dst = new byte[readLength + 1];
      buf.readBytes(dst, 0, readLength + 1);
      return RubyString.newString(getRuntime(), dst);
    }
    return getRuntime().getNil();
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
    try {
      bodyLatch.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException ignore) {
      return getRuntime().getNil();
    }
    if (buf.readableBytes() == 0)
      return getRuntime().getNil();
    int length;
    switch (args.length) {
      case 0:
        length = buf.readableBytes();
        break;
      case 1:
        int len = RubyInteger.num2int(args[0]);
        length = len > buf.readableBytes() ? buf.readableBytes() : len;
        break;
      default:
        len = RubyInteger.num2int(args[0]);
        length = len > buf.readableBytes() ? buf.readableBytes() : len;
    }
    byte[] dst = new byte[length];
    buf.readBytes(dst, 0, length);
    return RubyString.newString(getRuntime(), dst);
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
}
