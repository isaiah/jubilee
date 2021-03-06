require 'spec_helper'

feature "basic rack at non-root context" do

  before(:all) do
    configurator = Jubilee::Configuration.new(root: "#{apps_dir}/rack/basic", instances: 1)
    @server = Jubilee::Server.new(configurator.options)
    q = Queue.new
    @server.start{ q << 1 }
    q.pop
  end

  after(:all) do
    @server.stop
    sleep 0.1
  end

  it "should work for basic requests" do
    visit "/basic-rack"
    page.should have_content('it worked')
    page.find("#success")[:class].strip.should == 'basic-rack'
    page.find("#script_name").text.strip.should == ''
    page.find("#path_info").text.strip.should == '/basic-rack'
    page.find("#request_uri").text.strip.should == '/basic-rack'
  end

  it "should work for subcontext root with trailing slash" do
    visit "/basic-rack/"
    page.should have_content('it worked')
    page.find("#success")[:class].strip.should == 'basic-rack'
    page.find("#script_name").text.strip.should == ''
    page.find("#path_info").text.strip.should == '/basic-rack/'
    page.find("#request_uri").text.strip.should == '/basic-rack/'
  end

  it "should be running under the proper ruby version" do
    visit "/basic-rack/"
    page.find("#ruby-version").text.strip.should == RUBY_VERSION
  end

  it "should not have a vfs path for __FILE__" do
    visit "/basic-rack/"
    page.find("#path").text.strip.should_not match(/^vfs:/)
  end

  it "should not decode characters in URL" do
    visit "/basic-rack/foo%23%2C"
    page.find("#path_info").text.strip.should == '/basic-rack/foo%23%2C'
    page.find("#request_uri").text.strip.should == '/basic-rack/foo%23%2C'
  end

  it "should contain correct request headers" do
    uri = URI.parse("#{Capybara.app_host}/basic-rack/")
    Net::HTTP.start(uri.host, uri.port) do |http|
      accept = 'text/html;q=0.9,*/*;q=0.7'
      response = http.get(uri.request_uri, {'Accept' => accept})
      response.code.should == "200"
      response.body.should include("<div id='accept_header'>#{accept}</div>")
    end
  end

  it "should read post bodies via gets" do
    uri = URI.parse("#{Capybara.app_host}/basic-rack/gets")
    Net::HTTP.start(uri.host, uri.port) do |http|
      request = Net::HTTP::Post.new(uri.request_uri)
      request.form_data = {'field' => 'nothing'}
      response = http.request(request)
      response.body.should include("<div id='posted'>field=nothing</div>")
    end
  end

  it "should read post bodies via read" do
    uri = URI.parse("#{Capybara.app_host}/basic-rack/read")
    Net::HTTP.start(uri.host, uri.port) do |http|
      request = Net::HTTP::Post.new(uri.request_uri)
      request.form_data = {'field' => 'nothing'}
      response = http.request(request)
      response.body.should include("<div id='posted'>field=nothing</div>")
    end
  end

  it "should read post bodies via each" do
    uri = URI.parse("#{Capybara.app_host}/basic-rack/each")
    Net::HTTP.start(uri.host, uri.port) do |http|
      request = Net::HTTP::Post.new(uri.request_uri)
      request.form_data = {'field' => 'nothing'}
      response = http.request(request)
      response.body.should include("<div id='posted'>field=nothing</div>")
    end
  end
end
