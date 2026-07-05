#!/usr/bin/env bash
# Run this FROM YOUR OWN MACHINE (bash), not on the server. Builds both
# projects and pushes the artifacts to Hetzner over SSH. Assumes:
#   - key-based SSH access to the server already set up (same as for
#     gatto-piccolo.com deploys)
#   - /opt/voyage-log and /var/www/voyage-web already exist and are
#     owned by the right users (see README.md, step 2)
set -euo pipefail

SERVER="voyagelog@77.42.24.118"
BACKEND_DIR="./voyage-log"              # <- edit: path to your voyage-log checkout
FRONTEND_DIR="./voyage-web"             # <- edit: path to your voyage-web checkout

echo "==> Building backend jar"
(cd "$BACKEND_DIR" && ./gradlew bootJar -q)
JAR=$(find "$BACKEND_DIR/build/libs" -name '*.jar' ! -name '*-plain.jar' | head -1)
echo "    $JAR"

echo "==> Building frontend (production)"
(cd "$FRONTEND_DIR" && npm ci && npm run build -- --configuration production)

echo "==> Uploading backend jar"
scp "$JAR" "$SERVER:/opt/voyage-log/voyage-log.jar.new"
ssh "$SERVER" 'mv /opt/voyage-log/voyage-log.jar.new /opt/voyage-log/voyage-log.jar'

echo "==> Uploading frontend build"
rsync -avz --delete "$FRONTEND_DIR/dist/voyage-web/" "$SERVER:/var/www/voyage-web/"

echo "==> Restarting backend"
ssh "$SERVER" 'sudo systemctl restart voyage-log && sleep 2 && systemctl is-active voyage-log'

echo "==> Done. Tail logs with: ssh $SERVER 'journalctl -u voyage-log -f'"
