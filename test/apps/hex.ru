require_relative './persistent'

str = "This is longer and will be in hex"
run Persistent.new(["Hello", str])
