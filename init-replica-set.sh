#!/bin/bash
# Initialize MongoDB Replica Set for Transaction Support
#
# This script initializes a single-node replica set named "rs0"
# which is required for MongoDB transactions to work.
#
# The healthcheck in docker-compose.yml will automatically run this
# when the container starts.

echo "Waiting for MongoDB to start..."
sleep 5

echo "Initializing replica set..."
mongosh --eval '
try {
  rs.status();
  print("Replica set already initialized");
} catch (err) {
  rs.initiate({
    _id: "rs0",
    members: [
      { _id: 0, host: "localhost:27017" }
    ]
  });
  print("Replica set initialized successfully");
}
'

echo "Replica set initialization complete!"

