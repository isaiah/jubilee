package org.jruby.jubilee.impl;

import org.jruby.*;
import org.jruby.jubilee.Const;
import org.jruby.jubilee.RackResponse;
import org.jruby.runtime.*;
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
        if (statusCode instanceof RubyFixnum)
            this.statusCode = RubyNumeric.num2int(statusCode);
        else
            this.statusCode = Integer.parseInt(statusCode.asJavaString());
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

        try {
            // See Rack::Utils::STATUS_WITH_NO_ENTITY_BODY
            if (this.statusCode >= 200 && this.statusCode != 204 && this.statusCode != 205 && this.statusCode != 304) {
                if (this.contentLength != 0 && !contentLengthSet)
                    response.putHeader(Const.CONTENT_LENGTH, this.contentLength + "");
                else
                    response.setChunked(true);

                if (this.body.respondsTo("to_path"))
                    response.sendFile(body.callMethod(this.context, "to_path").asJavaString());
                else if (!this.body.isNil()) {
                    response.write(this.body.asJavaString());
                }
    //                RubyEnumerable.callEach19(runtime, this.context, this.body, Arity.OPTIONAL, new BlockCallback() {
    //                    @Override
    //                    public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block) {
    //                        if (args != null && !args[0].isNil()) {
    //                            response.write(args[0].asJavaString());
    ////                            String values = args[0].asJavaString();
    ////                            for (String value : values.split(Const.NEW_LINE)) {
    ////                                response.write(value);
    ////                            }
    //                        }
    //                        return runtime.getNil();
    //                    }
    //                });
            }

            response.end();
        } catch (ArrayIndexOutOfBoundsException e) {
            runtime.getOutputStream().println(e.getStackTrace());
        } finally {
            if (this.body.respondsTo("close"))
                this.body.callMethod(this.context, "close");
        }

    }
}
