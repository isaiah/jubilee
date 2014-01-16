require 'spec_helper'

feature "basic sinatra test" do

  before(:all) do
    configurator = Jubilee::Configuration.new(chdir: "#{apps_dir}/sinatra/basic")
    @server = Jubilee::Server.new(configurator.app, configurator.options)
    @server.start
  end

  after(:all) do
    @server.stop
  end

  it "should work" do
    visit ""
    page.should have_content('it worked')
    page.should have_selector('div.sinatra-basic')
    find("#success").should have_content('it worked')
  end

  it "should return a valid request scheme" do
    visit "/request-mapping"
    find("#success #scheme").text.should eql("http")
  end

  it "should return a static page beneath default 'public' dir" do
    visit "/some_page.html"
    page.find('#success')[:class].should == 'default'
  end

  it "should return 304 for unmodified static assets" do
    uri = URI.parse("#{Capybara.app_host}/some_page.html")
    mtime = File.mtime(File.expand_path("../../apps/sinatra/basic/public/some_page.html", __FILE__)).gmtime.strftime("%a, %d %b %Y %H:%M:%S GMT")
    Net::HTTP.start(uri.host, uri.port) do |http|
      request = Net::HTTP::Get.new(uri.request_uri)
      request.add_field('If-Modified-Since', mtime)
      response = http.request(request)
      response.code.should == "304"
    end
  end

  it "should post something" do
    visit "/poster"
    fill_in 'field', :with => 'something'
    click_button 'submit'
    find('#success').should have_content("you posted something")
  end


  it "should allow headers through" do
    uri = URI.parse("#{Capybara.app_host}/")
    response = Net::HTTP.get_response(uri)
    response['Biscuit'].should == 'Gravy'
  end

  it "should allow OPTIONS requests" do
    uri = URI.parse("#{Capybara.app_host}/")
    Net::HTTP.start(uri.host, uri.port) do |http|
      request = Net::HTTP::Options.new(uri.request_uri)
      response = http.request(request)
      response['access-control-allow-origin'].should == '*'
      response['access-control-allow-methods'].should == 'POST'
    end

  end

  it "should test Sir Postalot" do
    uri = URI.parse("#{Capybara.app_host}/poster")
    Net::HTTP.start(uri.host, uri.port) do |http|
      100.times do |i|
        http.request(Net::HTTP::Get.new(uri.request_uri))
        request = Net::HTTP::Post.new(uri.request_uri)
        request.form_data = {'field' => 'nothing'}
        response = http.request(request)
        response.body.should include("<div id='success'>you posted nothing</div>")
      end
    end
  end

end
