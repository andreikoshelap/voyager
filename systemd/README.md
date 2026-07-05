# Deploying voyage-log + voyage-web to Hetzner

Follows the same pattern already in use for gatto-piccolo.com on this box:
jar + systemd + native PostgreSQL for the backend (no Docker), nginx
serving the Angular build as static files and reverse-proxying `/api` and
`/telegram/webhook`. If this is a fresh Hetzner box rather than the
existing gatto-piccolo.com one, steps 1–2 (Java, Postgres, nginx, certbot)
may already be done — skip what's already installed.

## 0. Before you start

Check what's already listening so `SERVER_PORT=8083` in
`voyage-log.env.example` doesn't collide with portfell-admin (8081) or
whatever ledger-api runs on:

```bash
sudo ss -tlnp
```

Adjust the port in **both** `voyage-log.env` and the nginx config's
`proxy_pass` if 8083 is taken.

## 1. System packages (skip if already present)

```bash
sudo apt update
sudo apt install -y openjdk-21-jre-headless postgresql nginx certbot python3-certbot-nginx
```

## 2. Service user + directories

```bash
sudo useradd --system --home /opt/voyage-log --shell /usr/sbin/nologin voyagelog
sudo mkdir -p /opt/voyage-log /var/www/voyage-web
sudo chown voyagelog:voyagelog /opt/voyage-log
sudo chown www-data:www-data /var/www/voyage-web   # nginx just reads this
```

## 3. PostgreSQL (native, not Docker)

```bash
sudo -u postgres psql -c "CREATE ROLE voyagelog WITH LOGIN PASSWORD 'PICK_A_REAL_PASSWORD';"
sudo -u postgres psql -c "CREATE DATABASE voyagelog OWNER voyagelog;"
```

Use the same password in `voyage-log.env`'s `DB_PASSWORD`. Flyway
(V1–V4, already bundled in the jar via `spring-boot-starter-flyway`) runs
automatically on first startup — no manual migration step needed.

## 4. DNS

Point `voyage.gatto-piccolo.com` at the server's IP (A record, same zone
as gatto-piccolo.com). Wait for propagation before running certbot.

## 5. Backend env file

```bash
scp voyage-log.env.example voyagelog@SERVER:/tmp/
ssh voyagelog@SERVER 'sudo mv /tmp/voyage-log.env.example /opt/voyage-log/voyage-log.env'
ssh voyagelog@SERVER 'sudo chown voyagelog:voyagelog /opt/voyage-log/voyage-log.env && sudo chmod 600 /opt/voyage-log/voyage-log.env'
```

Then edit the real values on the server (`sudo -u voyagelog nano
/opt/voyage-log/voyage-log.env`): `DB_PASSWORD`, `TELEGRAM_BOT_TOKEN`,
`TELEGRAM_WEBHOOK_SECRET` (generate with `openssl rand -hex 16`).

## 6. systemd unit

```bash
sudo cp systemd/voyage-log.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable voyage-log
```

Don't start it yet — no jar has been uploaded.

## 7. nginx — first pass (HTTP only, for the certbot challenge)

```bash
sudo cp nginx/voyage.gatto-piccolo.com.conf /etc/nginx/sites-available/
sudo ln -s /etc/nginx/sites-available/voyage.gatto-piccolo.com.conf /etc/nginx/sites-enabled/
```

The file as written includes both the `:80` and `:443` blocks, but
`nginx -t` will fail on the `:443` block before a certificate exists.
Comment out the whole `server { listen 443 ... }` block for now:

```bash
sudo nginx -t && sudo systemctl reload nginx
```

## 8. TLS certificate

```bash
sudo mkdir -p /var/www/certbot
sudo certbot --nginx -d voyage.gatto-piccolo.com
```

Certbot edits the vhost in place to wire up the certificate — after it
runs, diff `/etc/nginx/sites-available/voyage.gatto-piccolo.com.conf`
against the version in this repo and reconcile if certbot rewrote
anything you want to keep (the `/api/` and `/telegram/webhook` blocks in
particular).

```bash
sudo nginx -t && sudo systemctl reload nginx
```

## 9. First deploy

From your own machine, edit `deploy.sh` (server address, local project
paths), then:

```bash
./deploy.sh
```

This builds the jar and the Angular production bundle, uploads both, and
restarts the service. On success:

```bash
ssh voyagelog@SERVER 'journalctl -u voyage-log -f'
```

Watch for `Started VoyageLogApplication` and (since `SPRING_PROFILES_ACTIVE=prod`)
a log line confirming the Telegram webhook was registered at
`https://voyage.gatto-piccolo.com/telegram/webhook`.

## 10. Verify end-to-end

```bash
curl -s https://voyage.gatto-piccolo.com/api/harbours | jq '.features | length'   # 68
curl -s https://voyage.gatto-piccolo.com/api/trips/active | jq
```

Open `https://voyage.gatto-piccolo.com` in a browser, and in Telegram
send `/start` to the bot — it should respond over the webhook now
(no more long-polling `local` profile).

## Ongoing deploys

Just `./deploy.sh` again. Flyway only applies new migration files, so
redeploying with no new `V*.sql` is a no-op on the schema.

## Rollback

Keep the previous jar around before overwriting:

```bash
ssh voyagelog@SERVER 'cp /opt/voyage-log/voyage-log.jar /opt/voyage-log/voyage-log.jar.bak'
```

If a deploy misbehaves: `ssh SERVER 'sudo cp /opt/voyage-log/voyage-log.jar.bak
/opt/voyage-log/voyage-log.jar && sudo systemctl restart voyage-log'`.
