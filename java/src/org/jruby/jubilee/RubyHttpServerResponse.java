package org.jruby.jubilee;

import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.vertx.java.core.http.HttpServerResponse;

/**
 * Created by isaiah on 21/12/2013.
 */
@JRubyClass(name = "HttpServerResponse")
public class RubyHttpServerResponse extends RubyObject {
    private HttpServerResponse resp;

    public static RubyClass createHttpServerResponseClass(final Ruby runtime) {
        RubyModule mJubilee = runtime.getOrCreateModule("Jubilee");
        RubyClass klazz = mJubilee.defineClassUnder("HttpResponse", runtime.getObject(), new ObjectAllocator() {
            @Override
            public IRubyObject allocate(Ruby ruby, RubyClass rubyClass) {
                return new RubyHttpServerResponse(ruby, rubyClass);
            }
        });
        klazz.defineAnnotatedMethods(RubyServer.class);
        return klazz;
    }

    public RubyHttpServerResponse(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }

    public RubyHttpServerResponse(Ruby ruby, RubyClass rubyClass, HttpServerResponse resp) {
        super(ruby, rubyClass);
        this.resp = resp;
    }

    @JRubyMethod
    public IRubyObject write(ThreadContext context, IRubyObject string) {
        this.resp.write(string.asJavaString());
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "status_code=")
    public IRubyObject setStatusCode(ThreadContext context, IRubyObject statusCode) {
        this.resp.setStatusCode(RubyNumeric.num2int(statusCode));
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "put_header")
    public IRubyObject putHeader(ThreadContext context, IRubyObject key, IRubyObject val) {
        this.resp.putHeader(key.asJavaString(), val.asJavaString());
        return context.runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject end(ThreadContext context) {
        this.resp.end();
        return context.runtime.getNil();
    }
}
