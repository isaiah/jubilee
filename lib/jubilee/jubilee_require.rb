# Redefine the require and load methods so we can make them synchronized

require 'jruby/synchronized'

module Kernel
  # make an alias of the original require
  alias_method :original_require, :require
  alias_method :original_load, :load

  def require(*params)
    org.vertx.java.platform.impl.JRubyVerticleFactory.requireCallback do
      #puts "in require callback"
      original_require(*params)
    end
  end

  def load(*params)
    org.vertx.java.platform.impl.JRubyVerticleFactory.requireCallback do
      #puts "in require callback"
      original_load(*params)
    end
  end

end
