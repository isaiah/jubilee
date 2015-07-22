require 'bundler/setup'
Bundler.require(:default)
require 'vertx/vertx'

def to_a(users)
  users.split("\0")
end

vertx = Jubilee.vertx
event_bus = vertx.event_bus()
sd = vertx.shared_data()

event_bus.consumer('logout') do |message|
  chat_data = sd.get_local_map("chat")
  users = to_a(chat_data.get("users"))
  users.reject!{|u| u == message.body }
  chat_data.put("users", users.join("\0"))
end


# register the user and return the previous users
event_bus.consumer('login') do |message|
  user = message.body
  chat_data = sd.get_local_map("chat")
    users = chat_data.get("users") || ""
  message.reply(users: to_a(users))
  users = user + "\0" + users
  chat_data.put("users", users)
  event_bus.publish("new_user", user)
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
