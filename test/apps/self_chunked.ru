require_relative './persistent'
body = ["5\r\nhello\r\n", "0\r\n\r\n"]
headers= {"X-Header" => "Works", 'Transfer-Encoding' => "chunked"}

run Persistent.new(body, headers)

