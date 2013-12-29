require 'test_helper'
require 'jubilee/cli'

class TestJubileeCLI < MiniTest::Unit::TestCase
  def test_parse_options
    cli = Jubilee::CLI.new(["app.rb"])
    cli.parse_options
    assert_equal "app.rb", cli.options[:rackup]
    assert_equal 8080, cli.options[:Port]
  end
end
