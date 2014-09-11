# Redefine the require and load methods so we can make them synchronized

require 'jruby/synchronized'

module Kernel
  # make an alias of the original require
  alias_method :original_require, :require
  alias_method :original_load, :load

  def require name
    org.vertx.java.platform.impl.JRubyVerticleFactory.requireCallback do
      #puts "in require callback"
      original_require name
    end
  end

  def load name
    org.vertx.java.platform.impl.JRubyVerticleFactory.requireCallback do
      #puts "in require callback"
      original_load name
    end
  end

end

