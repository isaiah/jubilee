module Jubilee
  module Version
    MAJOR = 2
    MINOR = 1
    PATCH = 0
    BUILD = "beta"

    STRING = [MAJOR, MINOR, PATCH, BUILD].compact.join('.')
  end
end
