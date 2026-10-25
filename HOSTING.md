# Hosting MPG Logger on Render

```
Browser ──► https://mpg-logger.onrender.com            (Render static site: the React app, free)
               │  /api/*  forwarded by Render
               ▼
            https://mpg-logger-backend.onrender.com    (Render web service: this Spring Boot app, $7 a month)
               │
               ▼
            Neon PostgreSQL                            (already set up, free)
```

The browser only ever talks to the static site's address. Render passes `/api/*` on to the backend
behind the scenes, so the app and the API share one origin and the session cookie works (see
"Sessions" in the README).

Both services are described in [`render.yaml`](render.yaml) (a Render "Blueprint"), so setting them up
is mostly clicking through. After that, **every push to `develop` redeploys** whichever repo changed.

**What it costs: $7 a month**, for the backend on the Starter plan. That plan never sleeps, so the app
answers straight away at any hour, and a new deploy only takes over once it's running (no downtime).
The static site and Neon are free.

---

## 1. Create a Render account and add your card

1. Go to <https://dashboard.render.com> and sign up with **GitHub**.
2. Allow Render to see the `mpg-logger-app` organization's repos (`mpg-logger-backend` and
   `mpg-logger-frontend`). If it only asks about your personal repos, use **Configure GitHub App**
   later in step 2 to add the organization.
3. Your workspace → **Billing** → add your card. The Starter plan needs it before step 2.

## 2. Create both services from the Blueprint

1. Dashboard → **New → Blueprint**.
2. Pick the **mpg-logger-backend** repo, branch **develop**. Render finds `render.yaml` and lists the two
   services it will create: `mpg-logger-backend` (web service, Starter) and `mpg-logger` (static site).
3. It asks for **DB_PASSWORD**: enter your Neon password. It's stored in Render, never in the repo.
4. **Apply** (or **Deploy Blueprint**). The first backend build takes about 5 minutes; the site about 1.

## 3. Check the backend's address

Open the **mpg-logger-backend** service. Under its name is its address. If it's exactly
`https://mpg-logger-backend.onrender.com`, skip to step 4.

If Render added something (e.g. `mpg-logger-backend-x7k2.onrender.com`, because the name was taken),
the site is forwarding to the wrong place. Put the real address in `render.yaml`:

```yaml
destination: https://mpg-logger-backend-x7k2.onrender.com/api/*
```

Commit and push that to `develop`; Render updates the site by itself.

## 4. Try it

1. Open the **mpg-logger** static site in the dashboard; its address is under the name, e.g.
   `https://mpg-logger.onrender.com` (with a suffix if the name was taken).
2. Sign in and log a fill-up. Reload the page: you should still be signed in.
3. On a phone, use **Add to Home Screen**: hosting over HTTPS is what makes the app installable.

From PowerShell:

```powershell
curl.exe https://mpg-logger.onrender.com/api/account
```

A `401` with `"detail":"Please sign in."` means the site, the backend and Neon are all working.

---

## Updating later

Push to `develop` in either repo. Render rebuilds that service and switches over once the new
version is running; the dashboard shows each deploy and its log.

## Costs

Prices as of late 2026 (check <https://render.com/pricing>; they change):

| Item | Cost | Notes |
|---|---|---|
| Backend, Starter web service | $7 a month | 0.5 CPU, 512 MB, always on. The app uses about 255 MB |
| Static site | Free | |
| Outgoing data | 5 GB a month included, then about $0.15 a GB | One person's use is far below |
| Neon | Free | The backend closes idle database connections after a minute, so Neon's compute can still sleep and stays within its free 100 compute-hours a month |

## Troubleshooting

| What you see | What to do |
|---|---|
| The Blueprint won't apply: "payment method required" | Add a card first (step 1.3). |
| Render can't see a repo | Dashboard → your workspace → **Settings → Git** (or the GitHub App's settings on GitHub) → give it access to the `mpg-logger-app` organization. |
| The backend's deploy fails | Open its **Logs**. A Hibernate "schema validation" error means the database is missing a table or column (run `db/schema.sql` or the migrations); a connection error usually means `DB_PASSWORD` is wrong (service → **Environment**). |
| The app says "Can't connect", or `/api/account` gives `404` | The site is forwarding to the wrong backend address: see step 3. |
| Signing in works but you're signed out on refresh | Use the static site's address, not the backend's: the cookie belongs to the site you signed in on. If it happens on the static site's address too, Render isn't passing the cookie through; the fix then is to serve the React app from the backend instead. |
| `/home` shows Render's "Not Found" | The `/*` → `/index.html` rewrite must stay the **last** route in `render.yaml`. |
| The backend runs out of memory (restarts, "OOM" in the log) | Move it to the next plan up (Standard, 2 GB) in the service's **Settings → Instance type**. |
