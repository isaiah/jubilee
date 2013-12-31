module Jubilee
  module Version
    MAJOR = 1
    MINOR = 1
    PATCH = 0
    BUILD = 'rc2'

    STRING = [MAJOR, MINOR, PATCH, BUILD].compact.join('.')
  end
end
