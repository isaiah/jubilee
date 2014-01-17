$:.unshift(File.join(File.dirname(__FILE__), "../lib"))
require 'minitest/autorun'
require 'minitest/unit'
require 'jubilee'
require 'tempfile'
require 'net/http'
require 'net/http/post/multipart'
require 'yaml'
def hit(uris)
  sleep 0.1
  uris.map do |u|
    res = Net::HTTP.get_response(URI(u))
    assert res != nil, "Didn't get a response: #{u}"
    res
  end
end

# which(1) exit codes cannot be trusted on some systems
# We use UNIX shell utilities in some tests because we don't trust
# ourselves to write Ruby 100% correctly :)
def which(bin)
  ex = ENV['PATH'].split(/:/).detect do |x|
    x << "/#{bin}"
    File.executable?(x)
  end or warn "`#{bin}' not found in PATH=#{ENV['PATH']}"
  ex
end

def redirect_test_io
  yield
  #orig_err = STDERR.dup
  #orig_out = STDOUT.dup
  #STDERR.reopen("test_stderr.#{$$}.log", "a")
  #STDOUT.reopen("test_stdout.#{$$}.log", "a")
  #STDERR.sync = STDOUT.sync = true

  #at_exit do
  #  File.unlink("test_stderr.#{$$}.log") rescue nil
  #  File.unlink("test_stdout.#{$$}.log") rescue nil
  #end

  #begin
  #  yield
  #ensure
  #  STDERR.reopen(orig_err)
  #  STDOUT.reopen(orig_out)
  #end
end

module Helpers
  attr_reader :status, :response

  def GET(path, header={})
    sleep 0.1
    Net::HTTP.start(@host, @port) { |http|
      user = header.delete(:user)
      passwd = header.delete(:passwd)

      get = Net::HTTP::Get.new(path, header)
      get.basic_auth user, passwd  if user && passwd
      http.request(get) { |response|
        @status = response.code.to_i
        begin
          @response = YAML.load(response.body)
        rescue TypeError, ArgumentError
          @response = nil
        end
      }
    }
  end

  def POST(path, formdata={}, header={})
    sleep 0.1
    Net::HTTP.start(@host, @port) { |http|
      user = header.delete(:user)
      passwd = header.delete(:passwd)

      post = Net::HTTP::Post.new(path, header)
      post.form_data = formdata
      post.basic_auth user, passwd  if user && passwd
      http.request(post) { |response|
        @status = response.code.to_i
        @response = YAML.load(response.body)
      }
    }
  end
end
