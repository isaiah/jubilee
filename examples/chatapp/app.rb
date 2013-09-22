require 'bundler/setup'
Bundler.require(:default)

get "/" do
  haml :index
end

__END__

@@layout
!!! html5
%html
  %head
    %script(src="/assets/javascripts/jquery.js" type="text/javascript")
    %script(src="/assets/javascripts/sockjs-0.3.4.min.js" type="text/javascript")
    %script(src="/assets/javascripts/vertxbus.js" type="text/javascript")
    %script(src="/assets/javascripts/application.js" type="text/javascript")
    %link(rel="stylesheet" href="/assets/stylesheets/application.css")
    %title jubilee demo

  %body
    = yield

@@index
#updates

%p
  %textarea#content(name="content" rows=8 cols=80)

%p
  %button#send Send

.users-list
