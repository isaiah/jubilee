require 'test_helper'
require 'jubilee/configuration'

class TestConfig < MiniTest::Unit::TestCase

  def setup
    @tmp = Tempfile.new("jubilee_config")
  end

  def teardown
    @tmp.close
    @tmp.unlink
  end

  def test_load
    @config = Jubilee::Configuration.new({rackup: "config/app.rb"})
    resp = [200, {"Content-Type" => "text/plain"}, ["embeded app"]]
    assert_equal resp, @config.app.call({})
  end

  def test_config_invalid
    @tmp.syswrite(%q(abcd "helloword"))
    assert_raises(NoMethodError) do
      Jubilee::Configuration.new(config_file: @tmp.path)
    end
  end

  def test_config_listen_on_port
    @tmp.syswrite(%q(listen 3000))
    options = Jubilee::Configuration.new(config_file: @tmp.path).options
    assert_equal(3000, options[:Port])
    assert_equal("0.0.0.0", options[:Host])
  end

  def test_config_listen_on_host_and_port
    @tmp.syswrite(%q(listen "localhost:3000"))
    options = Jubilee::Configuration.new(config_file: @tmp.path).options
    assert_equal(3000, options[:Port])
    assert_equal("127.0.0.1", options[:Host])
  end

  def test_config_ssl
    @tmp.syswrite(%q(ssl keystore: "keystore.jks", password: "helloworld"))
    options = Jubilee::Configuration.new(config_file: @tmp.path).options
    assert_equal("keystore.jks", options[:ssl_keystore])
    assert_equal("helloworld", options[:ssl_password])
    assert options[:ssl]
  end

  def test_config_eventbus
    @tmp.syswrite(%q(eventbus "/eb", inbound: [{}], password: [{}]))
    options = Jubilee::Configuration.new(config_file: @tmp.path).options
    assert_equal("/eb", options[:event_bus_prefix])
  end

  def test_config_non_exist
    path = @tmp.path
    @tmp.close!
    assert_raises(Errno::ENOENT) do
      Jubilee::Configuration.new(config_file: path)
    end
  end
end
