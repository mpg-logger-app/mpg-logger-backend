# Hosting MPG Logger for free

```
Browser ──► https://<your-site>.netlify.app            (Netlify: the React app, always on)
               │  /api/*  forwarded by Netlify
               ▼
            https://mpg-logger-backend-….run.app        (Google Cloud Run: this Spring Boot app)
               │
               ▼
            Neon PostgreSQL                             (already set up)
```

The browser only ever talks to the Netlify address. Netlify passes `/api/*` on to Cloud Run
behind the scenes, so the app and the API share one origin and the session cookie works
(see "Sessions" in the README).

What it costs: nothing, as long as you stay within the free allowances (see
[Staying free](#staying-free)). Google needs a billing account (a card) even for the free tier;
step 1.2 sets a budget alert so you'd hear about any charge.

Order matters: deploy the backend first (Netlify needs its address), then the frontend.

---

## Part 1 — Backend on Google Cloud Run

### 1.1 Create a project

1. Go to <https://console.cloud.google.com> and sign in with a Google account.
2. Top bar → project picker → **New project** → name it `mpg-logger` → **Create**.
3. Note the **Project ID** shown under the name (e.g. `mpg-logger-123456`).

### 1.2 Turn on billing, with a budget alert

1. Console → **Billing** → link a billing account to the project (add a card if asked).
2. **Billing → Budgets & alerts → Create budget**: scope = this project, amount = **$1**,
   alerts at 50 % / 100 %. You get an email if anything ever costs money.

### 1.3 Install the gcloud command-line tool

1. Download and run the installer: <https://cloud.google.com/sdk/docs/install> (Windows).
2. Open a new PowerShell window and run:

   ```powershell
   gcloud init                              # sign in, pick the mpg-logger project
   gcloud config set run/region us-east1    # a free-tier region, near Neon's us-east-2
   ```

### 1.4 Enable the services Cloud Run uses

```powershell
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com secretmanager.googleapis.com
```

### 1.5 Store the database password as a secret

The password never goes on the command line or into the repo.

1. Console → **Security → Secret Manager → Create secret**.
2. Name: `db-password`. Secret value: your Neon password. **Create**.
3. Let Cloud Run read it. Find your project number (Console → **Dashboard**, "Project number"), then:

   ```powershell
   gcloud secrets add-iam-policy-binding db-password `
     --member="serviceAccount:PROJECT_NUMBER-compute@developer.gserviceaccount.com" `
     --role="roles/secretmanager.secretAccessor"
   ```

### 1.6 Deploy

From the `mpg-logger-backend` folder, on the `develop` branch:

```powershell
gcloud run deploy mpg-logger-backend `
  --source . `
  --allow-unauthenticated `
  --memory 512Mi --cpu 1 `
  --min-instances 0 --max-instances 2 `
  --cpu-boost `
  --set-env-vars "DB_URL=jdbc:postgresql://ep-falling-hat-b52u8qyv-pooler.c-7.us-east-2.aws.neon.tech/mpgloggerdb?sslmode=require,DB_USERNAME=mpgloggerdb_owner" `
  --set-secrets "DB_PASSWORD=db-password:latest"
```

What the options mean:

| Option | Why |
|---|---|
| `--source .` | Uploads this folder; Cloud Build builds it with the [`Dockerfile`](Dockerfile). |
| `--allow-unauthenticated` | Netlify (and so your users) can reach it. The API still needs a signed-in session. |
| `--min-instances 0` | Scales to zero when nobody uses it: that's what keeps it free. |
| `--max-instances 2` | A cap, so a traffic spike can't run up a bill. |
| `--cpu-boost` | Extra CPU while starting, so the first request after a quiet period is faster. |

The first deploy takes a few minutes (it asks once to create an Artifact Registry repository: answer **Y**).
At the end it prints the **Service URL**, e.g. `https://mpg-logger-backend-abc123-ue.a.run.app`. Copy it.

### 1.7 Check it

```powershell
curl.exe https://mpg-logger-backend-abc123-ue.a.run.app/api/account
```

A `401` with `"detail":"Please sign in."` means it's running and talking to Neon.
The first call after a quiet period can take 10–20 seconds while it starts.

---

## Part 2 — Frontend on Netlify

### 2.1 Point the frontend at the backend

In the `mpg-logger-frontend` repo, open `netlify.toml` and replace
`REPLACE-WITH-YOUR-CLOUD-RUN-URL` with the host from step 1.6 (no `https://`, no trailing `/`):

```toml
to = "https://mpg-logger-backend-abc123-ue.a.run.app/api/:splat"
```

Commit and push that to `develop`.

### 2.2 Create the site

1. Go to <https://app.netlify.com> and sign up with **GitHub** (free).
2. **Add new site → Import an existing project → GitHub** → allow access to `mpg-logger-frontend`.
3. Branch to deploy: `develop`. The build settings are read from `netlify.toml`
   (`npm run build`, publish `dist`); leave them as they are. **Deploy**.
4. When it's done, **Site configuration → Change site name** to something like `mpg-logger-chandan`.
   Your app is now at `https://mpg-logger-chandan.netlify.app`.

### 2.3 Try it

Open the site, sign in, and log a fill-up. On a phone, use **Add to Home Screen**: hosting over
HTTPS is what makes the app installable.

---

## Updating later

- **Frontend:** push to `develop`; Netlify rebuilds and publishes by itself.
- **Backend:** run the same `gcloud run deploy …` command from step 1.6 again. Cloud Run switches
  over with no downtime.

## Staying free

Allowances as of late 2026 (check the providers' pricing pages; they change):

| Service | Free allowance | This app's use |
|---|---|---|
| Cloud Run | about 2 million requests and 180,000 vCPU-seconds a month, in us-central1 / us-east1 / us-west1 | A personal app uses a tiny fraction |
| Artifact Registry | 0.5 GB of stored images | Each deploy adds an image of about 250 MB, so delete old ones (below) |
| Secret Manager | a handful of secrets and accesses | One secret |
| Netlify | 100 GB bandwidth, 300 build minutes a month | Far below |
| Neon | 0.5 GB storage, 100 compute-hours a month | Far below |

Keep Artifact Registry under 0.5 GB: Console → **Artifact Registry → cloud-run-source-deploy →
mpg-logger-backend**, and delete all but the newest image after a deploy. (Or set a cleanup policy
there that keeps only the 2 most recent versions.)

## Troubleshooting

| What you see | What to do |
|---|---|
| Deploy fails with a permissions error about the build service account | Grant it the builder role: `gcloud projects add-iam-policy-binding PROJECT_ID --member="serviceAccount:PROJECT_NUMBER-compute@developer.gserviceaccount.com" --role="roles/cloudbuild.builds.builder"`, then deploy again. |
| The service won't start | `gcloud run services logs read mpg-logger-backend --limit 50`. A Hibernate "schema validation" error means the database is missing a table or column (run `db/schema.sql` or the migrations). |
| The app says "Can't connect" the first time after a quiet period | The backend was starting up (Netlify waits up to 26 s). Press **Try again**. |
| Signing in works but you're signed out on refresh | Use the Netlify address, not the `run.app` one: the cookie belongs to the site you signed in on. |
| `/home` shows Netlify's "Page not found" | `netlify.toml` must be at the root of the frontend repo (its last redirect sends every page to `index.html`). |
