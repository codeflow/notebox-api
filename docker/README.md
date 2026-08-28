# Running Notebox

The whole system — database, API and client — with data that survives a restart.

## First run

```bash
cd docker
./bootstrap.sh          # generates this deployment's secrets into ./data and .env
docker compose up -d --build
```

Then open **http://localhost:3000** and choose **Add account** to create your organization. You
become its first administrator, and you are signed in immediately.

| | |
|---|---|
| Client | http://localhost:3000 |
| API | http://localhost:8080/api |
| Database | localhost:**3307** (so it cannot collide with a MySQL you already run) |

## What persists, and where

Everything that must outlive a container is bind-mounted under `./data`, not hidden in a named
volume — you can see it, back it up, and delete it on purpose.

```
docker/data/mysql   the database
docker/data/keys    this deployment's JWT keypair
docker/.env         its database passwords and the secret-field master key
```

`docker compose down` keeps all of it. **`rm -rf data .env` is the only thing that does not** — and
it is irreversible: the master key in `.env` is what every stored Secret-flagged value was encrypted
with (C-12), and deleting it makes those values unreadable even with the database intact.

`bootstrap.sh` is safe to re-run: it never overwrites an existing key or `.env`, because rotating the
JWT key signs every live session out and rotating the master key loses the secrets.

## Things worth knowing

**The client's API URL is baked in at build time.** `NEXT_PUBLIC_*` values are inlined by Next, so
`NEXT_PUBLIC_API_BASE_URL` is a build argument. It must be the URL the **browser** can reach —
`localhost:8080`, not the compose service name, which only resolves inside the network. Change it
and rebuild: `docker compose up -d --build web`.

**CORS is not wildcarded.** The client is served from its own origin and calls the API directly, so
the API must be told which origin to accept — `NOTEBOX_CORS_ORIGINS`, defaulting to
`http://localhost:3000`. Point the client somewhere else and this has to move with it.

**Signup can be switched off.** `NOTEBOX_SIGNUP_ENABLED=false` makes `POST /organizations` refuse
with 403, for a deployment that provisions organizations by other means. It defaults to on, because
a self-hosted Notebox with no way in is not usable.

**The images do not run the tests.** `mvn verify` needs Docker itself (Testcontainers), and a build
that starts its own containers is a knot. Tests are the harness's job — `mvn -B verify` in the API
and `npm run verify` in the client. The API image *does* run Checkstyle, which needs no daemon.

## Everyday commands

```bash
docker compose logs -f api        # follow the API
docker compose up -d --build api  # rebuild just the API after a change
docker compose down               # stop everything, keep the data
docker compose down && rm -rf data .env && ./bootstrap.sh   # start completely over
```
