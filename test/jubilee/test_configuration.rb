require 'test_helper'
require 'jubilee/configuration'

class TestConfig < MiniTest::Unit::TestCase

  def setup
  end

  def test_load
    @config = Jubilee::Configuration.new({rackup: "config/app.rb"})
    resp = [200, {"Content-Type" => "text/plain"}, ["embeded app"]]
    assert_equal resp, @config.app.call({})
  end

  def test_config_invalid
    tmp = Tempfile.new("jubilee_config")
    tmp.syswrite(%q(abcd "helloword"))
    assert_raises(NoMethodError) do
      Jubilee::Configuration.new(config_file: tmp.path)
    end
  end

  def test_config_non_exist
    tmp = Tempfile.new("jubilee_config")
    path = tmp.path
    tmp.close!
    assert_raises(Errno::ENOENT) do
      Jubilee::Configuration.new(config_file: path)
    end
  end
end
