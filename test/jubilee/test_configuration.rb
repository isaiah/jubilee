require 'test_helper'
require 'jubilee/configuration'

class TestConfig < MiniTest::Unit::TestCase

  def setup
    @config = Jubilee::Configuration.new({rackup: "config/app.rb"})
  end

  def test_load
    resp = [200, {"Content-Type" => "text/plain"}, ["embeded app"]]
    assert_equal resp, @config.app.call({})
  end
end
