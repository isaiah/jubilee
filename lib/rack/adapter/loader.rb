module Rack
  module Adapter
    def self.load(config)
      rackup_code = File.read(config)
      eval("Rack::Builder.new {( #{rackup_code}\n )}.to_app", TOPLEVEL_BINDING, config)
    end
  end
end
