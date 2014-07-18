app = lambda do |env|
  data = env['rack.input'].read
  [200, {}, [data]]
end
run app
