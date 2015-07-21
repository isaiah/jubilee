Dir.glob(File.expand_path("../../jars/*.jar", __FILE__)) do |jar|
  require jar
end

require 'jubilee/jubilee.jar'
require 'rack'
require 'jubilee/version'
require 'jubilee/const'
require 'jubilee/server'
require 'jubilee/application'
require 'jubilee/configuration'
require 'jubilee/response'
require 'rack/handler/jubilee'
