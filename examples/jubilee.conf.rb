# Listen to port 3000
listen 3000

# enable https mode
ssl keystore: "keystore.jks", password: "hellojubilee"

pid "tmp/jubilee.pid"
stderr_path "log/jubilee.stderr.log"
stdout_path "log/jubilee.stdout.log"
