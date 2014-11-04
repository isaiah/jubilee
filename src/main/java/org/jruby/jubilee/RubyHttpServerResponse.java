package org.jruby.jubilee;

import org.jruby.*;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import java.util.Arrays;
import java.util.List;

/**
 * Created by isaiah on 21/12/2013.
 */
@JRubyClass(name = "HttpServerResponse")
public class RubyHttpServerResponse extends RubyObject {
    private HttpServerResponse resp;
    private HttpServerRequest req;
    private String lineSeparator;

    public static RubyClass createHttpServerResponseClass(final Ruby runtime) {
        RubyModule mJubilee = runtime.getOrCreateModule("Jubilee");
        RubyClass klazz = mJubilee.defineClassUnder("HttpServerResponse", runtime.getObject(), new ObjectAllocator() {
            @Override
            public IRubyObject allocate(Ruby ruby, RubyClass rubyClass) {
                return new RubyHttpServerResponse(ruby, rubyClass);
            }
        });
        klazz.defineAnnotatedMethods(RubyHttpServerResponse.class);
        return klazz;
    }

    public RubyHttpServerResponse(Ruby ruby, RubyClass rubyClass) {
        super(ruby, rubyClass);
    }

    public RubyHttpServerResponse(Ruby ruby, RubyClass rubyClass, HttpServerRequest request) {
        super(ruby, rubyClass);
        this.resp = request.response();
        this.req = request;
        this.lineSeparator = System.getProperty("line.separator");
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

    @JRubyMethod(name = "chunked=")
    public IRubyObject setChunked(ThreadContext context, IRubyObject chunked) {
        this.resp.setChunked(chunked.isTrue());
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "put_header")
    public IRubyObject putHeader(ThreadContext context, IRubyObject key, IRubyObject val) {
        String cookie = val.asJavaString();
        if (cookie.indexOf(this.lineSeparator) != -1)
            this.resp.putHeader(key.asJavaString(),
                    (List<String>) Arrays.asList(val.asJavaString().split(this.lineSeparator)));
        else this.resp.putHeader(key.asJavaString(), val.asJavaString());
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "send_file")
    public IRubyObject sendFile(ThreadContext context, IRubyObject filePath) {
        this.resp.sendFile(filePath.asJavaString());
        return context.runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject end(ThreadContext context) {
        this.resp.end();
        return context.runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject end(ThreadContext context, IRubyObject string) {
        this.resp.end(string.asJavaString());
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "put_default_headers")
    public IRubyObject putDefaultHeaders(ThreadContext context) {
        this.resp.putHeader("Server", Const.JUBILEE_VERSION);
        return context.runtime.getNil();
    }

    //TODO(isaiah) At the moment Vertx doesn't support hijack the response socket,
    // between request.response() and request.netSocket() only one can be invoked.
    @JRubyMethod(name = "net_socket")
    public IRubyObject netSocket(ThreadContext context) {
        RubyClass netSocketClass = (RubyClass) context.runtime.getClassFromPath("Jubilee::NetSocket");
        return new RubyNetSocket(context.runtime, netSocketClass, this.req.netSocket());
    }
}
