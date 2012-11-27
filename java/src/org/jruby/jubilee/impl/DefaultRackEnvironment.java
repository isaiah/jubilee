package org.jruby.jubilee.impl;

import org.jruby.RubyArray;
import org.jruby.RubyString;
import org.jruby.jubilee.RackEnvironment;
import org.jruby.jubilee.Const;
import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.jubilee.RackInput;
import org.vertx.java.core.http.HttpServerRequest;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: isaiah
 * Date: 11/26/12
 * Time: 11:40 AM
 */
public class DefaultRackEnvironment implements RackEnvironment {
    private RubyHash env;
    private Ruby runtime;

    public DefaultRackEnvironment( final Ruby runtime, final HttpServerRequest request, RackInput input)
    {
        this.runtime = runtime;
        // DEFAULT
        env = RubyHash.newHash(runtime);
        env.put("rack.version", Const.RackVersion(runtime));
        env.put("rack.errors", new RubyIORackErrors(runtime));
        env.put("rack.multithread", true);
        env.put("rack.multiprocess", false);
        env.put("rack.run_once", true);
        // FIXME hardcoded
        env.put("rack.url_scheme", "http");
        env.put("SCRIPT_NAME", "");

        // Rack blows up if this is an empty string, and Rack::Lint
        // blows up if it's nil. So 'text/plain' seems like the most
        // sensible default value.
        env.put("CONTENT_TYPE", "text/plain");

        env.put("QUERY_STRING", "");
        env.put(Const.SERVER_PROTOCOL, Const.HTTP_11);
        env.put(Const.SERVER_SOFTWARE, Const.JUBILEE_VERSION);
        env.put(Const.GATEWAY_INTERFACE, Const.CGI_VER);

        // Parse request headers
        Map<String, String> headers = request.headers();
        String host;
        if ((host = headers.get(Const.HTTP_HOST)) != null) {
            int colon = host.indexOf(":");
            if (colon > 0) {
                env.put(Const.SERVER_NAME, host.substring(0, colon));
                env.put(Const.SERVER_PORT, host.substring(colon + 1));
            } else {
                env.put(Const.SERVER_NAME, host);
                env.put(Const.SERVER_PORT, Const.PORT_80);
            }

        } else {
            env.put(Const.SERVER_NAME, Const.LOCALHOST);
            env.put(Const.SERVER_PORT, Const.PORT_80);
        }
        env.put(Const.RACK_INPUT, input);
        env.put(Const.REQUEST_METHOD, request.method);
        env.put(Const.REQUEST_PATH, request.path);
        env.put(Const.REQUEST_URI, request.uri);
        env.put(Const.QUERY_STRING, orEmtpy(request.query));
        env.put(Const.HTTP_HOST, headers.get("host"));
        env.put(Const.HTTP_COOKIE, orEmtpy(headers.get("cookie")));
        env.put(Const.HTTP_USER_AGENT, headers.get("user-agent"));
        env.put(Const.HTTP_ACCEPT, headers.get("accept"));
        env.put(Const.HTTP_ACCEPT_LANGUAGE, orEmtpy(headers.get("accept-language")));
        env.put(Const.HTTP_ACCEPT_ENCODING, orEmtpy(headers.get("accept-encoding")));
        env.put(Const.HTTP_CONNECTION, orEmtpy(headers.get("connection")));
        env.put(Const.PATH_INFO, request.path);
        env.put(Const.SERVER_SOFTWARE, "jubilee 0.0.1");
    }

    public RubyHash getEnv() {
        return env;
    }

    private RubyString orEmtpy(String jString) {
        return jString == null ? RubyString.newEmptyString(runtime) : RubyString.newString(runtime, jString);
    }
/*
    private RubyHash createRubyHash( final Request request )
    {
        RubyHash env = request.getRubyHeaders();
        assignConnectionRelatedCgiHeaders( env, request );
        tweakCgiVariables( env, request.getUri() );
        updateEnv( env, request );
        return env;
    }

    void updateEnv( final RubyHash env, final Request request )
    {
        env.put( "rack.version", Version.RACK );
        env.put( "rack.input", getRackInput() );
        env.put( "rack.errors", new RubyIORackErrors( _runtime ) );
        env.put( "rack.multithread", true );
        env.put( "rack.multiprocess", false );
        env.put( "rack.run_once", false );
        env.put( "rack.url_scheme", request.getUrl().getProtocol() );
    }

    @Override
    public InputStream getInput()
    {
        return _stream;
    }

    @Override
    public int getContentLength()
    {
        return _request.getBodyString().length();
    }

    @Override
    public RackInput getRackInput()
    {
        return _input;
    }

    @Override
    public void setRackInput( final RackInput input )
    {
        _input = input;
    }

    @Override
    public RubyHash toRuby()
    {
        return createRubyHash( _request );
    }

    void assignConnectionRelatedCgiHeaders( final RubyHash env, final Request request )
    {
        InetSocketAddress remoteAddress = (InetSocketAddress) request.getRemoteAddress();
        String remote = remoteAddress.getHostName().replace( "/", "" );
        env.put( "REMOTE_ADDR", remote );
        if( !env.containsKey( "SERVER_NAME" ) && !env.containsKey( "SERVER_PORT" ) )
        {
            if( request.containsHeader( HttpHeaders.Names.HOST ) )
            {
                String[] parts = request.getHeader( HttpHeaders.Names.HOST ).split( ":" );
                if( parts.length > 0 )
                {
                    env.put( "SERVER_NAME", parts[0] );
                    if( parts.length > 1 )
                    {
                        env.put( "SERVER_PORT", parts[1] );
                    }
                }
            }
            else
            {
                InetSocketAddress localAddress = (InetSocketAddress) request.getLocalAddress();
                env.put( "SERVER_NAME", localAddress.getHostName() );
                env.put( "SERVER_PORT", String.valueOf( localAddress.getPort() ) );
            }
        }
        env.put( "SERVER_PROTOCOL", request.getHttpRequest().getProtocolVersion().toString() );
        env.put( "HTTP_VERSION", request.getHttpRequest().getProtocolVersion().toString() );
    }

    void tweakCgiVariables( final RubyHash env, final String path )
    {
        // Rack-specified rules
        if( env.get( "SCRIPT_NAME" ) == null )
            env.put( "SCRIPT_NAME", "" );
        if( env.get( "SCRIPT_NAME" ).equals( "/" ) )
            env.put( "SCRIPT_NAME", "" );
        if( env.get( "PATH_INFO" ) != null && env.get( "PATH_INFO" ).equals( "" ) )
        {
            env.remove( "PATH_INFO" );
        }
//        else
//        {
//            int snLen = env.get( "SCRIPT_NAME" ).toString().length();
//            env.put( "PATH_INFO", path.substring( snLen, path.length() ) );
//        }
//        if( !env.containsKey( "REQUEST_PATH" ) ||
//                env.get( "REQUEST_PATH" ) == null ||
//                env.get( "REQUEST_PATH" ).toString().length() == 0 )
//        {
//            env.put( "REQUEST_PATH", env.get( "SCRIPT_NAME" ).toString() + env.get( "PATH_INFO" ) );
//        }
        if( !env.containsKey( "SERVER_PORT" ) )
            env.put( "SERVER_PORT", "80" );

        // CGI-specific headers
        env.put( "REQUEST_URI", path );
        env.put( "GATEWAY_INTERFACE", "CGI/1.2" );
        env.put( "SERVER_SOFTWARE", "Aspen " + Version.ASPEN );
    }

*/
}
