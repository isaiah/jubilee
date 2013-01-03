$:.unshift(File.join(File.dirname(__FILE__), "../lib"))
require 'minitest/autorun'
require 'minitest/unit'
require 'jubilee'
require 'net/http'
require 'net/http/post/multipart'
require 'yaml'
def hit(uris)
  uris.map do |u|
    res = nil

    if u.kind_of? String
      res = Net::HTTP.get(URI.parse(u))
    else
      url = URI.parse(u[0])
      res = Net::HTTP.new(url.host, url.port).start {|h| h.request(u[1]) }
    end

    assert res != nil, "Didn't get a response: #{u}"
    res
  end
end

module Helpers
  attr_reader :status, :response

  def GET(path, header={})
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
