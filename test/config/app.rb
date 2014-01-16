class App
  def initialize(resp = nil)
    if resp
      @resp = resp.is_a?(Array) ? resp : [resp]
    else
      @resp = ["embedded app"]
    end
  end

  def call(env)
    [200, {"Content-Type" => "text/plain"}, @resp]
  end
end
