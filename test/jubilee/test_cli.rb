require 'test_helper'
require 'jubilee/cli'

class TestJubileeCLI < MiniTest::Unit::TestCase
  def test_parse_options
    cli = Jubilee::CLI.new(["app.ru"])
    cli.parse_options
    assert_equal "app.ru", cli.options[:rackup]
  end

  def test_chdir
    cli = Jubilee::CLI.new(["--dir", "test", "app.ru"])
    cli.parse_options
    assert_equal "test", cli.options[:chdir]
  end

  def test_eventbus_prefix
    cli = Jubilee::CLI.new(["--eventbus", "/eb"])
    cli.parse_options
    assert_equal "/eb", cli.options[:eventbus_prefix]
  end
end
