# Hosting MPG Logger on a Google Cloud VM

```
Browser ──► https://mpg-logger.duckdns.org         (one Google Cloud e2-micro VM, always on)
               │
               ├─ /api/*  ──► Spring Boot           (this repo's Dockerfile)
               │                 │
               │                 ▼
               │              Neon PostgreSQL       (already set up)
               │
               └─ everything else ──► the React app (built files, from the mpg-logger-frontend repo)
```

Both run in Docker on the VM, started by [`deploy/docker-compose.yml`](deploy/docker-compose.yml).
**Caddy** ([`Caddyfile`](https://github.com/mpg-logger-app/mpg-logger-frontend/blob/develop/Caddyfile)
in the frontend repo) is the web server in front: it serves the React app, forwards `/api/*` to Spring
Boot, and gets the HTTPS certificate by itself. The browser only ever sees one address, so the app and
the API share one origin and the session cookie works (see "Sessions" in the README).

The VM runs all the time, so there are no cold starts: the app answers straight away at any hour.

**What it costs: about $3.65 a month.** The e2-micro VM and its 30 GB disk are in Google's Always
Free tier; the only charge is the VM's public IPv4 address ($0.005 an hour). Details in
[Staying cheap](#staying-cheap).

The images are built on your PC (the VM is too small to build them), pushed to GitHub's container
registry (free for public images), and pulled by the VM. [`scripts/deploy.ps1`](scripts/deploy.ps1) does
that in one go.

What you need on your PC: Docker Desktop (running), both repos checked out side by side
(`mpg-logger-app\mpg-logger-backend` and `mpg-logger-app\mpg-logger-frontend`), and a GitHub account in
the `mpg-logger-app` organization.

---

## 1. Google Cloud project

### 1.1 Create a project

1. Go to <https://console.cloud.google.com> and sign in with a Google account.
2. Top bar → project picker → **New project** → name it `mpg-logger` → **Create**.
3. Note the **Project ID** shown under the name (e.g. `mpg-logger-123456`).

### 1.2 Turn on billing, with a budget alert

1. Console → **Billing** → link a billing account to the project (add a card if asked).
2. **Billing → Budgets & alerts → Create budget**: scope = this project, amount = **$5**,
   alerts at 50 %, 90 % and 100 %. The expected bill is about $3.65, so an email means something
   is costing more than it should.

### 1.3 Install the gcloud command-line tool

1. Download and run the installer: <https://cloud.google.com/sdk/docs/install> (Windows).
2. Open a new PowerShell window and run:

   ```powershell
   gcloud init                                      # sign in, pick the mpg-logger project
   gcloud config set compute/zone us-east1-b        # a free-tier region, near Neon's us-east-2
   gcloud services enable compute.googleapis.com
   ```

## 2. Create the VM

From PowerShell:

```powershell
# A fixed public address, so it stays the same if the VM restarts.
gcloud compute addresses create mpg-logger-ip --region us-east1

gcloud compute instances create mpg-logger `
  --zone us-east1-b `
  --machine-type e2-micro `
  --image-family debian-12 --image-project debian-cloud `
  --boot-disk-size 30GB --boot-disk-type pd-standard `
  --address mpg-logger-ip `
  --tags web

# Let web traffic in (80 for the HTTPS certificate check and redirect, 443 for the app).
gcloud compute firewall-rules create allow-web --allow tcp:80,tcp:443,udp:443 --target-tags web

# Show the address; you need it in the next step.
gcloud compute addresses describe mpg-logger-ip --region us-east1 --format "value(address)"
```

| Option | Why |
|---|---|
| `--machine-type e2-micro` in `us-east1` | The Always Free machine (2 shared vCPUs, 1 GB memory). Free only in us-east1, us-central1 and us-west1. |
| `--boot-disk-type pd-standard` | The free disk type. The console's default ("balanced") is **not** free. |
| `--boot-disk-size 30GB` | The free allowance; more than enough. |

## 3. Give it a name (free, with DuckDNS)

HTTPS certificates are issued for names, not bare IP addresses.

1. Go to <https://www.duckdns.org> and sign in (with GitHub or Google).
2. Add a subdomain, e.g. `mpg-logger` → you get `mpg-logger.duckdns.org`.
3. In **current ip**, enter the VM's address from step 2 → **update ip**.

(If you'd rather have your own domain, buy one, add an `A` record pointing at the VM's address, and use
that name instead everywhere below.)

## 4. Set up the VM

Open a terminal on the VM (the first time, gcloud creates an SSH key for you):

```powershell
gcloud compute ssh mpg-logger --zone us-east1-b
```

Then, on the VM:

```bash
# Docker, from Docker's official install script.
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER

# 1 GB of swap, so a memory spike slows things down instead of stopping the app.
sudo fallocate -l 1G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Install security updates automatically.
sudo apt-get install -y unattended-upgrades

# The folder the app runs from.
mkdir -p ~/mpg-logger
exit
```

## 5. First deploy

### 5.1 Settings (including the database password)

On your PC, in this repo:

1. Copy [`deploy/.env.example`](deploy/.env.example) to `deploy/.env` and fill in `SITE_ADDRESS`
   (your DuckDNS name) and `DB_PASSWORD` (your Neon password). `.env` is in `.gitignore`, so the
   password never goes into the repo.
2. Copy it to the VM and make it readable only by you:

   ```powershell
   gcloud compute scp deploy\.env mpg-logger:mpg-logger/.env --zone us-east1-b
   gcloud compute ssh mpg-logger --zone us-east1-b --command "chmod 600 mpg-logger/.env"
   ```

### 5.2 Sign in to GitHub's container registry

1. GitHub → your picture → **Settings → Developer settings → Personal access tokens → Tokens (classic)
   → Generate new token (classic)**. Scope: `write:packages`. Copy the token.
2. On your PC:

   ```powershell
   docker login ghcr.io -u <your-github-username>     # paste the token as the password
   ```

### 5.3 Build and push the images

From this repo, on `develop` in both repos:

```powershell
docker compose -f deploy/docker-compose.yml build
docker compose -f deploy/docker-compose.yml push
```

The first time, GitHub creates the two packages as **private**. Make them public, so the VM can pull
them without a password (the images hold no secrets: the password is only in `.env` on the VM):
<https://github.com/orgs/mpg-logger-app/packages> → `mpg-logger-backend` → **Package settings →
Change visibility → Public**. Do the same for `mpg-logger-frontend`.

### 5.4 Start it

```powershell
.\scripts\deploy.ps1
```

It builds and pushes again (quick: nothing changed), copies `docker-compose.yml` to the VM, and starts
the app there.

### 5.5 Check it

Open `https://mpg-logger.duckdns.org` (your name), sign in, and log a fill-up. On a phone, use
**Add to Home Screen**: HTTPS is what makes the app installable.

From PowerShell:

```powershell
curl.exe https://mpg-logger.duckdns.org/api/account
```

A `401` with `"detail":"Please sign in."` means Caddy, Spring Boot and Neon are all working.
The backend takes about a minute to start after a deploy; until then you get a `502`.

---

## Updating later

Pull `develop` in both repos, then:

```powershell
.\scripts\deploy.ps1
```

It deploys whatever is checked out in both repos. The app is unavailable for about a minute while the
new backend starts (the app shows "Can't connect"; **Try again** once it's up).

## Looking after the VM

Each command runs on the VM from your PC:

| To | Run |
|---|---|
| See if both containers are running | `gcloud compute ssh mpg-logger --command "cd mpg-logger && docker compose ps"` |
| Read the backend's log | `gcloud compute ssh mpg-logger --command "cd mpg-logger && docker compose logs --tail 100 backend"` |
| Read Caddy's log (HTTPS problems) | `gcloud compute ssh mpg-logger --command "cd mpg-logger && docker compose logs --tail 100 web"` |
| See memory use | `gcloud compute ssh mpg-logger --command "docker stats --no-stream"` |
| Restart the app | `gcloud compute ssh mpg-logger --command "cd mpg-logger && docker compose restart"` |
| Change a setting in `.env` | Edit `deploy\.env`, copy it as in 5.1, then restart the app |

The containers restart by themselves if they crash or the VM reboots. Docker's logs are capped at
30 MB per container, so they can't fill the disk.

## Staying cheap

Prices as of late 2026 (check Google's pricing pages; they change):

| Item | Cost | Notes |
|---|---|---|
| e2-micro VM | Free | One per account, in us-east1 / us-central1 / us-west1 only |
| 30 GB standard disk | Free | Only the `pd-standard` type |
| Public IPv4 address | about $3.65 a month | $0.005 an hour while the VM runs. If you **stop** the VM, the reserved address costs $0.01 an hour; release it (`gcloud compute addresses delete mpg-logger-ip --region us-east1`) if you stop for long |
| Network out | Free up to 1 GB a month, then about $0.12 a GB | One person's use is far below |
| GitHub container registry | Free | Public images |
| DuckDNS | Free | |
| Neon | Free | The backend closes idle database connections after a minute, so Neon's compute can still sleep and stays within the free 100 compute-hours a month |

## Troubleshooting

| What you see | What to do |
|---|---|
| The browser can't reach the site at all | Check DuckDNS points at the VM's address (step 3) and the firewall rule exists (`gcloud compute firewall-rules list`). |
| A certificate warning, or Caddy's log mentions "challenge failed" | Caddy couldn't prove it owns the name: same checks as above (port 80 must be open). Fix them and restart the app; Caddy tries again. |
| `502 Bad Gateway` | The backend is starting (wait a minute) or has stopped: read its log. A Hibernate "schema validation" error means the database is missing a table or column (run `db/schema.sql` or the migrations). |
| `deploy.ps1` stops at `docker compose pull` with "denied" | The packages are still private: make them public (5.3). |
| `push` says "denied" or "unauthorized" | Sign in to ghcr.io again (5.2); the token needs `write:packages`. |
| The build stops downloading base images ("connection reset", "EOF") | A network problem reaching Docker Hub. Try again later, or pull the base images through Google's mirror once, e.g. `docker pull mirror.gcr.io/library/node:24-alpine` and `docker tag mirror.gcr.io/library/node:24-alpine node:24-alpine` (same for `caddy:2.10-alpine`, `eclipse-temurin:21-jdk` and `eclipse-temurin:21-jre`). |
| Signing in works but you're signed out on refresh | Use the DuckDNS name, not the IP address: the cookie belongs to the name you signed in on. |
| The backend keeps restarting and `docker stats` shows it at its 600 MB limit | Something is using more memory than expected; read its log. Raising `mem_limit` in `deploy/docker-compose.yml` is possible but leaves less for Caddy and the system. |

## Trying it on your PC

The same compose file runs on your PC with Docker Desktop: put your Neon values in `deploy/.env` with
`SITE_ADDRESS=http://localhost`, then `docker compose -f deploy/docker-compose.yml up --build` and open
<http://localhost>. Stop it with `docker compose -f deploy/docker-compose.yml down`.
