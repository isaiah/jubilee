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
    private int contentLength = 0;
    private boolean contentLengthSet = false;
    private ThreadContext context;

    public DefaultRackResponse(ThreadContext context, IRubyObject statusCode, IRubyObject headers, IRubyObject body) {
        this.context = context;
        this.statusCode = RubyNumeric.num2int(statusCode);
        this.body = body;
        this.headers = headers.convertToHash();

        if (body instanceof RubyArray && ((RubyArray) body).getLength() == 1)
            this.contentLength = ((String) ((RubyArray) body).get(0)).length();
    }
    @Override
    public void respond(final HttpServerResponse response) {
        final Ruby runtime = context.runtime;
        response.setStatusCode(this.statusCode);

        this.headers.visitAll(new RubyHash.Visitor() {
            @Override
            public void visit(IRubyObject key, IRubyObject val) {
                if (key.asJavaString().equals(Const.CONTENT_LENGTH)) {
                    contentLengthSet = true;
                }
                response.putHeader(key.asJavaString(), val.asJavaString());
            }
        });
        // FIXME: other no body response
        if (this.statusCode > 200) {
            if (this.contentLength != 0 && !contentLengthSet)
                response.putHeader(Const.CONTENT_LENGTH, this.contentLength + "");
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
        }

        try {
            response.end();
        } finally {
            if (this.body.respondsTo("close"))
                this.body.callMethod(this.context, "close");
        }

    }
}
