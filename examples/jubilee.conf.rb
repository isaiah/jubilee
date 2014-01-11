# Listen to port 3000
listen 3000

# enable https mode
ssl keystore: "keystore.jks", password: "hellojubilee"

# the directory where the rack app seats in
working_directory "chatapp"

# rack environment
environment "development"

eventbus "/eventbus", inbound: [{}], outbound: [{}]

clustering true

debug true
