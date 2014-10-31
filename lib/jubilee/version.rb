module Jubilee
  module Version
    MAJOR = 2
    MINOR = 1
    PATCH = 0
    BUILD = "rc1"

    STRING = [MAJOR, MINOR, PATCH, BUILD].compact.join('.')
  end
end
