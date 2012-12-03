package org.jruby.jubilee.impl;

import org.jruby.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.jubilee.Const;
import org.jruby.jubilee.RackInput;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Created with IntelliJ IDEA.
 * User: isaiah
 * Date: 11/26/12
 * Time: 10:12 PM
 */
public class RubyIORackInput extends RubyObject implements RackInput {
  private BlockingQueue<String> buf;
  private List<RubyString> backup;
  private int pos;
  private boolean isEnd;

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

  public RubyIORackInput(Ruby runtime, BlockingQueue<String> buf) {
    this(runtime, createRubyIORackInputClass(runtime));
    this.buf = buf;
    pos = 0;
    backup = new ArrayList<RubyString>();
    isEnd = false;
  }

  /**
   * gets must be called without arguments and return a string, or nil on EOF.
   *
   * @param context it's a JRuby thing
   * @return a string, or nil on EOF
   */
  @Override
  @JRubyMethod
  public IRubyObject gets(ThreadContext context) {
    IRubyObject empty = getRuntime().getNil();
    if (! isEnd) {
      if (pos < backup.size()) {
        pos++;
        return backup.get(pos - 1);
      }
      getRuntime().getOutputStream().println("=======gets======");
      try {
        String part = buf.take();

        getRuntime().getOutputStream().println(part);
        getRuntime().getOutputStream().println("=======gets======");
        if (part.equals(Const.END_OF_BODY)) {
          isEnd = true;
          return empty;
        }
        RubyString ret = RubyString.newString(getRuntime(), part);
        backup.add(ret);
        pos++;
        return ret;
      } catch (InterruptedException e) {
        return getRuntime().getNil();
      }
    } else {
      // rack should not enter this part
      return empty;
    }
  }

  /**
   * read behaves like IO#read. Its signature is read([length, [buffer]]). If given,
   * length must be an non-negative Integer (>= 0) or nil, and buffer must be a
   * String and may not be nil. If length is given and not nil, then this method
   * reads at most length bytes from the input stream. If length is not given or
   * nil, then this method reads all data until EOF. When EOF is reached, this
   * method returns nil if length is given and not nil, or "" if length is not
   * given or is nil. If buffer is given, then the read data will be placed into
   * buffer instead of a newly created String object.
   *
   * @param context it's a JRuby thing
   * @param args    [length, [buffer]]
   * @return nil if length is given and not nil, or "" if length is not given or nil
   */
  @Override
  @JRubyMethod(optional = 2)
  // FIXME this is broken
  public IRubyObject read(ThreadContext context, IRubyObject[] args) {
    return getRuntime().getNil();
    //Ruby runtime = getRuntime();
    //if (pos >= buffer.length()) {
    //  return runtime.getNil();
    //}
    //if (args.length == 0) {
    //  return RubyString.newString(runtime, "");
    //}
    //int length = RubyInteger.num2int(args[0].convertToInteger());
    //if (pos + length >= buffer.length()) {
    //  length = buffer.length() - pos;
    //}
    //RubyString ret = RubyString.newString(runtime, "");
    //pos += length;
    //if (args.length == 2) {
    //  RubyString buffer = args[1].convertToString();
    //  buffer.append19(ret);
    //  return runtime.getNil();
    //}
    //return ret;
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
    for (RubyString str: backup) {
      block.yield(context, str);
    }
    while (! isEnd)
      block.yield(context, gets(context));
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
    pos = 0;
    return getRuntime().getNil();
  }

  /**
   * Close the input. Exposed only to the Java side because the Rack spec says
   * that application code must not call close, so we don't expose a close method to Ruby.
   */
  @JRubyMethod
  public IRubyObject close(ThreadContext context) {
    buf.clear();
    backup.clear();
    return getRuntime().getNil();
  }
}
