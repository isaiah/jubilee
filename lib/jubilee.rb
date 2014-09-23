# this is only used when it's running as a rubygem
Dir[File.expand_path("../../jars/*.jar", __FILE__)].each{|jar| require jar}

require 'jubilee/jubilee.jar'
require 'rack'
require 'jubilee/version'
require 'jubilee/const'
require 'jubilee/server'
require 'jubilee/application'
require 'jubilee/configuration'
require 'jubilee/response'
require 'rack/handler/jubilee'

module Jubilee
end
