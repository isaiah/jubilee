require 'stringio'

require 'const'
require 'null_io'

include Jubilee::Const

def load_rack_adapter(options)
  config = File.join(options[:chdir], 'config.ru')
  #rackup_code = File.read(config)
  app, opts = Rack::Builder.parse_file config
  #app = eval("Rack::Builder.new {( #{rackup_code}\n )}.to_app", TOPLEVEL_BINDING, config)
  Rack::Lint.new(Rack::CommonLogger.new(app, STDOUT))
end

def normalize_env(env, req)
  proto_env
end

options = {
  chdir: File.join(File.dirname(__FILE__), "../../spec"),
  environment: 'development'
  #environment: ENV['RACK_ENV'] || 'development',
}
host, port, app = DEFAULT_HOST, DEFAULT_PORT, load_rack_adapter(options)

proto_env = {
  "rack.version".freeze => Rack::VERSION,
  "rack.errors".freeze => StringIO.new,
  "rack.multithread".freeze => true,
  "rack.multiprocess".freeze => false,
  "rack.run_once".freeze => true,
  # FIXME hardcoded
  "rack.url_scheme".freeze => "http",
  "SCRIPT_NAME".freeze => "",

  # Rack blows up if this is an empty string, and Rack::Lint
  # blows up if it's nil. So 'text/plain' seems like the most
  # sensible default value.
  "CONTENT_TYPE".freeze => "text/plain",

  "QUERY_STRING".freeze => "",
  SERVER_PROTOCOL => HTTP_11,
  SERVER_SOFTWARE => JUBILEE_VERSION,
  GATEWAY_INTERFACE => CGI_VER
}


#args.each do |arg|
#  case arg.class
#  when Fixnum, /^\d+$/ then port = arg.to_i
#  when String then host = arg
#  when Hash then options = arg
#  else
#    @app = arg if arg.respond_to?(:call)
#  end
#end
server = HttpServer.new
server.request_handler do |req|
  body = Buffer.create(0)
  req.data_handler do |buf|
    body.append_buffer(buf)
  end

  req_headers = req.headers
  #req_body = body.get_string(0, body.length)
  req_body = NullIO.new
  env = proto_env
  env[RACK_INPUT] = req_body
  env[REQUEST_METHOD] = req.method
  env[REQUEST_PATH] = req.path
  env[REQUEST_URI] = "http://" + req_headers["host"] + req.uri
  env[QUERY_STRING] = req.query || ""
  env[HTTP_HOST] = req_headers["host"]
  env[HTTP_COOKIE] = req_headers["cookie"] || ""
  env[HTTP_USER_AGENT] = req_headers["user-agent"]
  env[HTTP_ACCEPT] = req_headers["accept"] || ""
  env[HTTP_ACCEPT_LANGUAGE] = req_headers["accept-language"] || ""
  env[HTTP_ACCEPT_ENCODING] = req_headers["accept-encoding"] || ""
  env[HTTP_CONNECTION] = req_headers["connection"] || ""
  env[PATH_INFO] = req.uri
  env[SERVER_NAME], env[SERVER_PORT] = req_headers["host"].split(":")
  env[SERVER_SOFTWARE] = "jubilee 0.0.1(#{JRUBY_VERSION})"

  #req.end_handler do
  status, headers, resp_body = app.call(env)
  req.response.status_code = status.to_i
  headers.each {|k,v| req.response.headers[k] = v}

  resp_body.each do |part|
    # TODO check if chunked
    req.response.write_buffer(Buffer.create_from_str(part))
  end
  #end
  req.response.end
end
server.listen(port, host)

puts "Jubilee Web Server started, Press CTRL+C to stop"
