require 'bundler/setup'
Bundler.require(:default)
require 'vertx'

def to_a(shared_set)
  ret = []
  shared_set.each{ |item| ret << item}
  ret
end

Vertx::EventBus.register_handler('logout') do |message|
  Vertx::SharedData.get_set(:users).delete(message.body)
end

# register the user and return the pervious users
Vertx::EventBus.register_handler('login') do |message|
  user = message.body
  users = Vertx::SharedData.get_set(:users).add(user)
  message.reply(users: to_a(users))
  Vertx::EventBus.publish("new_user", user)
end


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
  %label receiver
  %select#receivers(name="receiver")
    %option all
%p
  %textarea#content(name="content" rows=8 cols=80)

%p
  %button#send Send

.users-list
