require 'test_helper'
require 'jubilee/configuration'

class TestConfig < MiniTest::Unit::TestCase

  def setup
    @tmp = Tempfile.new("jubilee_config")
    @resp = [200, {"Content-Type" => "text/plain"}, ["embedded app"]]
    @dir = Dir.getwd
  end

  def teardown
    @tmp.close
    @tmp.unlink
    Dir.chdir(@dir)
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
    @tmp.syswrite(%q(eventbus "/eb", inbound: [{}], outbound: [{}]))
    options = Jubilee::Configuration.new(config_file: @tmp.path).options
    assert_equal("/eb", options[:eventbus_prefix])
  end

  # This will trigger the initialization of vertx cluster manager so the rake
  # task won't quit at the end
  #def test_config_file_clustering_true
  #  @tmp.syswrite(%q(clustering true))
  #  options = Jubilee::Configuration.new(config_file: @tmp.path).options
  #  assert_equal("0.0.0.0", options[:cluster_host])
  #end

  #def test_config_file_clustering_host_and_port
  #  @tmp.syswrite(%q(clustering "localhost:5701"))
  #  options = Jubilee::Configuration.new(config_file: @tmp.path).options
  #  assert_equal("127.0.0.1", options[:cluster_host])
  #  assert_equal(5701, options[:cluster_port])
  #end

  def test_config_file_working_directory
    @tmp.syswrite(%q(working_directory "chatapp"))
    options = Jubilee::Configuration.new(config_file: @tmp.path).options
    assert_match(/chatapp/, options[:chdir])
  end

  def test_config_file_environment
    @tmp.syswrite(%q(environment "test"))
    options = Jubilee::Configuration.new(config_file: @tmp.path).options
    assert_equal("test", options[:environment])
  end

  def test_config_non_exist
    path = @tmp.path
    @tmp.close!
    assert_raises(Errno::ENOENT) do
      Jubilee::Configuration.new(config_file: path)
    end
  end
end
