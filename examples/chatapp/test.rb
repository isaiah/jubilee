require 'vertx'

Vertx.set_timer(5000) do |timer_id|
  puts 'another verticle'
end
