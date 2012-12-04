$:.unshift(File.join(File.dirname(__FILE__), "../lib"))
require 'minitest/autorun'
require 'minitest/unit'
require 'jubilee'
require 'net/http'
require 'net/http/post/multipart'
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
