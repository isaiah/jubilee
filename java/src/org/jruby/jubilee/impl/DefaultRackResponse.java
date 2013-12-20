package org.jruby.jubilee.impl;

import org.jruby.*;
import org.jruby.jubilee.Const;
import org.jruby.jubilee.RackResponse;
import org.jruby.runtime.Arity;
import org.jruby.runtime.JavaInternalBlockBody;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.vertx.java.core.http.HttpServerResponse;

/**
 * Created by isaiah on 20/12/2013.
 */
public class DefaultRackResponse implements RackResponse {
    private int statusCode;
    private RubyHash headers;
    private IRubyObject body;
    private int contentLength;
    private ThreadContext context;

    public DefaultRackResponse(ThreadContext context, IRubyObject statusCode, IRubyObject headers, IRubyObject body) {
        this.context = context;
        this.statusCode = RubyNumeric.num2int(statusCode);
        this.body = body;
        this.headers = headers.convertToHash();

        if (body instanceof RubyArray && ((RubyArray) body).getLength() == 1)
            this.contentLength = ((RubyString) ((RubyArray) body).get(0)).strLength();
    }
    @Override
    public void respond(final HttpServerResponse response) {
        final Ruby runtime = context.runtime;
        response.setStatusCode(this.statusCode);
        this.headers.visitAll(new RubyHash.Visitor() {
            @Override
            public void visit(IRubyObject key, IRubyObject val) {
                if (key.asJavaString().equals(Const.CONTENT_LENGTH)) {
                    contentLength = RubyNumeric.num2int(val);
                } else {
                    response.putHeader(key.asJavaString(), val.asJavaString());
                }
            }
        });
        if (this.statusCode < 200)
            response.end();
        if (this.contentLength != 0)
            response.putHeader(Const.CONTENT_LENGTH, new Integer(this.contentLength).toString());
        else
            response.setChunked(true);

        if (this.body.respondsTo("to_path"))
            response.sendFile(body.callMethod(this.context, "to_path").asJavaString());
        else
            RubyEnumerable.each(this.context, this.body, new JavaInternalBlockBody(runtime, Arity.ONE_REQUIRED) {
                @Override
                public IRubyObject yield(ThreadContext context, IRubyObject fragment) {
                    response.write(fragment.asJavaString());
                    return runtime.getTrue();
                }
            });
        if (this.body.respondsTo("close"))
            this.body.callMethod(this.context, "close");
    }
}
