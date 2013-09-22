# Listen to port 3000
listen 3000

# the number of workers to serve client
worker_threads 4

# the ssl certification path
ssl_keystore "jubilee/keystore.jks"

# the ssl certification key
ssl_password "helloworld"

pid "tmp/jubilee.pid"
stderr_path "log/jubilee.stderr.log"
stdout_path "log/jubilee.stdout.log"
