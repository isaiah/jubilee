require 'vertx'
require 'rack'
include Vertx
DEFAULT_HOST = 'localhost'
DEFAULT_PORT = 3000

def load_rack_adapter(options)
  config = File.join(options[:chdir], 'config.ru')
  rackup_code = File.read(config)
  eval("Rack::Builder.new {( #{rackup_code}\n )}.to_app", TOPLEVEL_BINDING, config)
end

options = {
  chdir: Dir.pwd,
  environment: 'development'
  #environment: ENV['RACK_ENV'] || 'development',
}
host, port, app = DEFAULT_HOST, DEFAULT_PORT, load_rack_adapter(options)

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

  req.end_handler do
    status, headers, resp = app.call(body)
    req.response.status_code = status
    headers.each {|k,v| req.response.headers[k] = v}
    req.response.write_str(resp.first)
  end
end
server.listen(port, host)

puts "Jubilee Web Server started, Press CTRL+C to stop"
