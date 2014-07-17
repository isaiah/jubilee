$:.unshift(File.expand_path("../../lib", __FILE__))
require 'jubilee'
require 'capybara/poltergeist'
require 'capybara/rspec'
require 'net/http'

Capybara.app_host = "http://localhost:8080"
Capybara.run_server = false
Capybara.default_driver = :poltergeist

def apps_dir
  File.expand_path("../apps", __FILE__)
end
