require 'jubilee'
require 'capybara/poltergeist'
require 'capybara/rspec'
require 'ostruct'
require 'net/http'

Capybara.app_host = "http://localhost:3215"
Capybara.run_server = false
Capybara.default_driver = :poltergeist

def apps_dir
  File.expand_path("../apps", __FILE__)
end
