#!/usr/bin/env bash
# Fast front-end loop.
#
# The database and API stay in containers; the CLIENT runs on the host with Next's hot reload, so a
# change shows up in under a second instead of a two-minute image rebuild.
#
# Running the client in a container with the source bind-mounted would also work in principle, but
# file watching through Docker's macOS filesystem is slow and often needs polling — which defeats
# the point of the exercise. The host watches natively.
#
# Nothing else changes: .env.local already points the client at http://localhost:8080/api, and the
# API's CORS already allows http://localhost:3000, so the containerised API serves the host client
# exactly as it serves the containerised one.
set -euo pipefail
cd "$(dirname "$0")"

echo "starting the database and API…"
docker compose up -d db api

# The web container owns port 3000; the dev server needs it.
if docker compose ps --services --filter status=running | grep -qx web; then
  echo "stopping the web container so the dev server can take port 3000"
  docker compose stop web
fi

echo
echo "client → http://localhost:3000   (hot reload)"
echo "api    → http://localhost:8080/api"
echo "stop with Ctrl+C; 'docker compose up -d --build web' puts the container back"
echo

cd ../../notebox-web
exec npm run dev
