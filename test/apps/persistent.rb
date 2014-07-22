class Persistent
  attr_reader :headers, :body

  def initialize(body = ["Hello"], headers = { "X-Header" => "Works" })
    @body = body
    @headers = headers
  end


  def call(env)
    status = Integer(env['HTTP_X_STATUS'] || 200)
    [status, headers, body]
  end
end
