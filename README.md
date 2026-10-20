
# API Uptime Monitor

An uptime and API monitoring platform in the style of UptimeRobot, built with **Java 17, Spring Boot 3, Maven, MySQL and React (Vite)**.

Users add their websites or API URLs, and the backend pings them at a configurable interval, saving the response time and status in MySQL. When a website goes down, the system opens an incident and sends an alert by email, Slack or Discord webhook. The React dashboard shows live status, uptime percentages and response-time charts.

---

## Features

- **Authentication:** register and log in with JWT (Spring Security, BCrypt password hashing). Roles: `USER` and `ADMIN`. The first registered user becomes `ADMIN`.
- **Monitor management:** create, edit, delete, pause and resume monitors. Configure the URL, HTTP method (GET or HEAD), check interval (10 seconds to 1 hour), timeout and expected status code.
- **Check now:** trigger an immediate check from the dashboard.
- **Scheduled health checks:** a scheduler finds due monitors every 5 seconds and runs the checks in parallel on a fixed thread pool.
- **Check history:** every check stores its status, HTTP status code, response time and error message.
- **Incident tracking:** a monitor is marked `DOWN` after 2 consecutive failures, which opens an incident. When the monitor recovers, the incident is closed with the total downtime.
- **Alerts:** console log, optional email (SMTP), and a Slack or Discord webhook per monitor, on both DOWN and recovery.
- **Statistics:** uptime percentage for 24 hours, 7 days and 30 days, average response time, and a dashboard summary.
- **Charts:** response-time chart (Recharts) with 1h, 24h, 7d and 30d ranges. Failed checks are highlighted in red.
- **Data retention:** old check results (30 days by default) are removed automatically every night at 3 AM.
- **API quality:** request validation, centralized exception handling, Swagger / OpenAPI docs and JUnit tests.

## Tech Stack

| Layer    | Technology                                                                  |
| -------- | --------------------------------------------------------------------------- |
| Backend  | Java 17, Spring Boot 3, Spring Security, Spring Data JPA / Hibernate, Maven |
| Database | MySQL 8                                                                     |
| Auth     | JWT (jjwt), BCrypt                                                          |
| Frontend | React 18, Vite, React Router, Axios, Recharts                               |
| Docs     | springdoc-openapi (Swagger UI)                                              |
| Testing  | JUnit 5                                                                     |

## Architecture

```
React  --JWT-->  REST API (Spring Security)  -->  MySQL
                      |
        MonitorScheduler (@Scheduled, every 5s)
                      |  finds due monitors
                      v
        ExecutorService (10 threads)  -->  HttpCheckService (java.net.http.HttpClient)
                      |
                      v
        MonitorCheckProcessor:  save result -> update status -> open/close incident -> AlertService
```

Key design decisions:

1. **The HTTP call runs outside the database transaction.** A slow website never holds a database connection.
2. **`nextCheckAt` is updated before dispatching.** The scheduler never dispatches the same monitor twice, and an in-flight set prevents overlapping checks.
3. **Failure threshold of 2.** A single temporary glitch does not trigger a false alarm, which avoids flapping between UP and DOWN.
4. **Alerts are sent after the transaction commits.** No email or webhook call happens inside a database transaction.
5. **Chart downsampling.** If a range has more than 300 points, it is thinned out, but failed checks are always kept.
6. **Ownership checks.** Every monitor query is scoped to the owner, so users can never access each other's data.

## Project Structure

```
api-uptime-monitor/
├── backend/                    Spring Boot (Maven)
│   ├── Dockerfile
│   └── src/main/java/com/aman/uptime/
│       ├── config/             Security, CORS, thread pool
│       ├── security/           JWT service and filter
│       ├── entity/             User, Monitor, CheckResult, Incident
│       ├── repository/         Spring Data JPA repositories
│       ├── dto/                Request and response records
│       ├── service/            Business logic (checks, alerts, stats)
│       ├── scheduler/          MonitorScheduler (@Scheduled)
│       ├── controller/         REST controllers
│       └── exception/          Global error handler
├── frontend/                   React + Vite
│   └── src/                    pages/, components/, api.js, auth.jsx
├── database/schema.sql         Optional manual MySQL setup
├── docker-compose.yml          Optional local MySQL
└── README.md
```

## Getting Started (Local)

### Prerequisites

| Tool    | Version                    |
| ------- | -------------------------- |
| JDK     | 17 or higher               |
| Maven   | 3.8+                       |
| Node.js | 18+                        |
| MySQL   | 8.x (running on port 3306) |

If you do not have MySQL installed but have Docker, run `docker compose up -d` in the project root. It starts MySQL 8 with user `root` and password `root`.

### 1. Start the backend (port 8080)

```bash
cd backend
mvn spring-boot:run
```

You can also use `start-backend.bat` (Windows) or `./start-backend.sh` (macOS / Linux) from the project root.

### 2. Start the frontend (port 5173)

```bash
cd frontend
npm install
npm run dev
```

You can also use `start-frontend.bat` or `./start-frontend.sh`.

### 3. Open the app

Go to **http://localhost:5173**, create an account on the Register page, and log in. There are no hardcoded or demo users.

### Database configuration

- The database `uptime_monitor` and all tables are created automatically on first run (`spring.jpa.hibernate.ddl-auto=update`). No manual setup is needed.
- The default connection is `root` / `root` on `localhost:3306`.
- If your MySQL credentials are different, set environment variables before starting the backend:
  - Windows PowerShell: `$env:DB_PASSWORD="your_password"`
  - Windows cmd: `set DB_PASSWORD=your_password`
  - macOS / Linux: `export DB_PASSWORD=your_password`
- Never commit your real password to the repository. Always pass it through environment variables.
- If you prefer to create the tables manually, run `database/schema.sql` in MySQL.

### Useful links

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health endpoint: http://localhost:8080/api/health

## Configuration

### Backend environment variables

| Variable                                                           | Purpose                                                 | Default                      |
| ------------------------------------------------------------------ | ------------------------------------------------------- | ---------------------------- |
| `DB_URL`                                                         | Full JDBC URL (overrides the host, port and name below) | built from the values below  |
| `DB_HOST`                                                        | MySQL host                                              | `localhost`                |
| `DB_PORT`                                                        | MySQL port                                              | `3306`                     |
| `DB_NAME`                                                        | Database name                                           | `uptime_monitor`           |
| `DB_USERNAME`                                                    | MySQL user                                              | `root`                     |
| `DB_PASSWORD`                                                    | MySQL password                                          | `root`                     |
| `DB_PARAMS`                                                      | Extra JDBC parameters (for example, SSL options)        | local-friendly defaults      |
| `JWT_SECRET`                                                     | Secret used to sign tokens (32+ characters)             | development value, change it |
| `CORS_ORIGINS`                                                   | Extra allowed origins, comma separated                  | `http://localhost:5173`    |
| `PORT`                                                           | Server port                                             | `8080`                     |
| `ALERT_EMAIL_ENABLED`                                            | Turn on email alerts                                    | `false`                    |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP settings                                           | Gmail SMTP host and port     |

`*.vercel.app` and `*.netlify.app` origins are already allowed by CORS. Railway MySQL variables (`MYSQLHOST`, `MYSQLPORT`, `MYSQLUSER`, `MYSQLPASSWORD`, `MYSQLDATABASE`) are picked up automatically.

### Frontend environment variable

| Variable         | Purpose                                      |
| ---------------- | -------------------------------------------- |
| `VITE_API_URL` | Backend base URL, needed only for deployment |

### Enabling email alerts

```
ALERT_EMAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_gmail_app_password
```

## API Overview

| Method | Endpoint                         | Description                         | Auth   |
| ------ | -------------------------------- | ----------------------------------- | ------ |
| POST   | `/api/auth/register`           | Create an account                   | Public |
| POST   | `/api/auth/login`              | Log in and receive a JWT            | Public |
| GET    | `/api/auth/me`                 | Current user                        | JWT    |
| GET    | `/api/monitors`                | List your monitors                  | JWT    |
| POST   | `/api/monitors`                | Create a monitor                    | JWT    |
| GET    | `/api/monitors/{id}`           | Get one monitor                     | JWT    |
| PUT    | `/api/monitors/{id}`           | Update a monitor                    | JWT    |
| DELETE | `/api/monitors/{id}`           | Delete a monitor and its history    | JWT    |
| POST   | `/api/monitors/{id}/pause`     | Pause a monitor                     | JWT    |
| POST   | `/api/monitors/{id}/resume`    | Resume a monitor                    | JWT    |
| POST   | `/api/monitors/{id}/check`     | Run a check immediately             | JWT    |
| GET    | `/api/monitors/{id}/results`   | Check history (`?hours=24`)       | JWT    |
| GET    | `/api/monitors/{id}/stats`     | Uptime and response-time statistics | JWT    |
| GET    | `/api/monitors/{id}/incidents` | Incidents for a monitor             | JWT    |
| GET    | `/api/dashboard`               | Dashboard summary                   | JWT    |
| GET    | `/api/incidents`               | Recent incidents (`?limit=20`)    | JWT    |
| GET    | `/api/health`                  | Service health                      | Public |
| GET    | `/api/admin/overview`          | System-wide counts                  | ADMIN  |

Full interactive documentation is available in Swagger UI.

## Database Schema

| Table             | Purpose                                               |
| ----------------- | ----------------------------------------------------- |
| `users`         | Accounts, hashed passwords and roles                  |
| `monitors`      | Monitor settings and current status (owned by a user) |
| `check_results` | One row per executed check                            |
| `incidents`     | Downtime periods with start, end and reason           |

Relationships: a user has many monitors, and a monitor has many check results and incidents.

## Deployment

Vercel and Netlify host only the **frontend**. The Spring Boot backend needs a Java host, so the app is deployed in three parts. All three have free tiers.

| Part     | Where                          | How                                                                                          |
| -------- | ------------------------------ | -------------------------------------------------------------------------------------------- |
| MySQL    | Railway, Aiven or Clever Cloud | Create a MySQL service and note the host, user and password                                  |
| Backend  | Render or Railway (Docker)     | Connect the GitHub repo and set the root directory to `backend` (a Dockerfile is included) |
| Frontend | Vercel or Netlify              | Root directory `frontend`, build command `npm run build`, output `dist`                |

**Backend environment variables** (set in the Render or Railway dashboard):

```
DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD   (or a single DB_URL)
JWT_SECRET=<a long random string, 32+ characters>
```

- Tables are created on startup. The `PORT` variable is provided by the host.
- If your cloud MySQL requires SSL (for example, Aiven), set `DB_PARAMS=useSSL=true&serverTimezone=UTC`.
- For a custom frontend domain, add it to `CORS_ORIGINS`.

**Frontend environment variable** (set in the Vercel or Netlify dashboard):

```
VITE_API_URL=https://your-backend.onrender.com
```

`vercel.json` and `netlify.toml` are included so that page refreshes work with client-side routing.

> Note: Render's free tier puts the backend to sleep after about 15 minutes without traffic, which pauses monitoring. For continuous 24/7 monitoring, use an always-on plan or Railway.

## Troubleshooting

| Problem                           | Solution                                                                 |
| --------------------------------- | ------------------------------------------------------------------------ |
| `Access denied for user 'root'` | Your MySQL password differs from the default. Set `DB_PASSWORD`.       |
| `Communications link failure`   | MySQL is not running. Start it (or run `docker compose up -d`).        |
| Port 8080 or 5173 already in use  | Change `server.port` or stop the other process.                        |
| Monitor stays "Pending"           | Wait 5-10 seconds for the first check to finish.                         |
| `npm install` fails on Windows  | Run `npm cache clean --force`, delete `node_modules`, and try again. |

## Roadmap

- Redis caching and rate limiting
- Public status pages (`/status/{slug}`)
- CI/CD with GitHub Actions
- More tests (Mockito for `MonitorCheckProcessor`, `@DataJpaTest`)
- SSRF protection: block private IP ranges (localhost, 10.x, 192.168.x) in production. They are currently allowed to make local testing easy.

# API Uptime Monitor

An uptime and API monitoring platform in the style of UptimeRobot, built with **Java 17, Spring Boot 3, Maven, MySQL and React (Vite)**.

Users add their websites or API URLs, and the backend pings them at a configurable interval, saving the response time and status in MySQL. When a website goes down, the system opens an incident and sends an alert by email, Slack or Discord webhook. The React dashboard shows live status, uptime percentages and response-time charts.

---

## Features

- **Authentication:** register and log in with JWT (Spring Security, BCrypt password hashing). Roles: `USER` and `ADMIN`. The first registered user becomes `ADMIN`.
- **Monitor management:** create, edit, delete, pause and resume monitors. Configure the URL, HTTP method (GET or HEAD), check interval (10 seconds to 1 hour), timeout and expected status code.
- **Check now:** trigger an immediate check from the dashboard.
- **Scheduled health checks:** a scheduler finds due monitors every 5 seconds and runs the checks in parallel on a fixed thread pool.
- **Check history:** every check stores its status, HTTP status code, response time and error message.
- **Incident tracking:** a monitor is marked `DOWN` after 2 consecutive failures, which opens an incident. When the monitor recovers, the incident is closed with the total downtime.
- **Alerts:** console log, optional email (SMTP), and a Slack or Discord webhook per monitor, on both DOWN and recovery.
- **Statistics:** uptime percentage for 24 hours, 7 days and 30 days, average response time, and a dashboard summary.
- **Charts:** response-time chart (Recharts) with 1h, 24h, 7d and 30d ranges. Failed checks are highlighted in red.
- **Data retention:** old check results (30 days by default) are removed automatically every night at 3 AM.
- **API quality:** request validation, centralized exception handling, Swagger / OpenAPI docs and JUnit tests.

## Tech Stack

| Layer    | Technology                                                                  |
| -------- | --------------------------------------------------------------------------- |
| Backend  | Java 17, Spring Boot 3, Spring Security, Spring Data JPA / Hibernate, Maven |
| Database | MySQL 8                                                                     |
| Auth     | JWT (jjwt), BCrypt                                                          |
| Frontend | React 18, Vite, React Router, Axios, Recharts                               |
| Docs     | springdoc-openapi (Swagger UI)                                              |
| Testing  | JUnit 5                                                                     |

## Architecture

```
React  --JWT-->  REST API (Spring Security)  -->  MySQL
                      |
        MonitorScheduler (@Scheduled, every 5s)
                      |  finds due monitors
                      v
        ExecutorService (10 threads)  -->  HttpCheckService (java.net.http.HttpClient)
                      |
                      v
        MonitorCheckProcessor:  save result -> update status -> open/close incident -> AlertService
```

Key design decisions:

1. **The HTTP call runs outside the database transaction.** A slow website never holds a database connection.
2. **`nextCheckAt` is updated before dispatching.** The scheduler never dispatches the same monitor twice, and an in-flight set prevents overlapping checks.
3. **Failure threshold of 2.** A single temporary glitch does not trigger a false alarm, which avoids flapping between UP and DOWN.
4. **Alerts are sent after the transaction commits.** No email or webhook call happens inside a database transaction.
5. **Chart downsampling.** If a range has more than 300 points, it is thinned out, but failed checks are always kept.
6. **Ownership checks.** Every monitor query is scoped to the owner, so users can never access each other's data.

## Project Structure

```
api-uptime-monitor/
├── backend/                    Spring Boot (Maven)
│   ├── Dockerfile
│   └── src/main/java/com/aman/uptime/
│       ├── config/             Security, CORS, thread pool
│       ├── security/           JWT service and filter
│       ├── entity/             User, Monitor, CheckResult, Incident
│       ├── repository/         Spring Data JPA repositories
│       ├── dto/                Request and response records
│       ├── service/            Business logic (checks, alerts, stats)
│       ├── scheduler/          MonitorScheduler (@Scheduled)
│       ├── controller/         REST controllers
│       └── exception/          Global error handler
├── frontend/                   React + Vite
│   └── src/                    pages/, components/, api.js, auth.jsx
├── database/schema.sql         Optional manual MySQL setup
├── docker-compose.yml          Optional local MySQL
└── README.md
```

## Getting Started (Local)

### Prerequisites

| Tool    | Version                    |
| ------- | -------------------------- |
| JDK     | 17 or higher               |
| Maven   | 3.8+                       |
| Node.js | 18+                        |
| MySQL   | 8.x (running on port 3306) |

If you do not have MySQL installed but have Docker, run `docker compose up -d` in the project root. It starts MySQL 8 with user `root` and password `root`.

### 1. Start the backend (port 8080)

```bash
cd backend
mvn spring-boot:run
```

You can also use `start-backend.bat` (Windows) or `./start-backend.sh` (macOS / Linux) from the project root.

### 2. Start the frontend (port 5173)

```bash
cd frontend
npm install
npm run dev
```

You can also use `start-frontend.bat` or `./start-frontend.sh`.

### 3. Open the app

Go to **http://localhost:5173**, create an account on the Register page, and log in. There are no hardcoded or demo users.

### Database configuration

- The database `uptime_monitor` and all tables are created automatically on first run (`spring.jpa.hibernate.ddl-auto=update`). No manual setup is needed.
- The default connection is `root` / `root` on `localhost:3306`.
- If your MySQL credentials are different, set environment variables before starting the backend:
  - Windows PowerShell: `$env:DB_PASSWORD="your_password"`
  - Windows cmd: `set DB_PASSWORD=your_password`
  - macOS / Linux: `export DB_PASSWORD=your_password`
- Never commit your real password to the repository. Always pass it through environment variables.
- If you prefer to create the tables manually, run `database/schema.sql` in MySQL.

### Useful links

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health endpoint: http://localhost:8080/api/health

## Configuration

### Backend environment variables

| Variable                                                           | Purpose                                                 | Default                      |
| ------------------------------------------------------------------ | ------------------------------------------------------- | ---------------------------- |
| `DB_URL`                                                         | Full JDBC URL (overrides the host, port and name below) | built from the values below  |
| `DB_HOST`                                                        | MySQL host                                              | `localhost`                |
| `DB_PORT`                                                        | MySQL port                                              | `3306`                     |
| `DB_NAME`                                                        | Database name                                           | `uptime_monitor`           |
| `DB_USERNAME`                                                    | MySQL user                                              | `root`                     |
| `DB_PASSWORD`                                                    | MySQL password                                          | `root`                     |
| `DB_PARAMS`                                                      | Extra JDBC parameters (for example, SSL options)        | local-friendly defaults      |
| `JWT_SECRET`                                                     | Secret used to sign tokens (32+ characters)             | development value, change it |
| `CORS_ORIGINS`                                                   | Extra allowed origins, comma separated                  | `http://localhost:5173`    |
| `PORT`                                                           | Server port                                             | `8080`                     |
| `ALERT_EMAIL_ENABLED`                                            | Turn on email alerts                                    | `false`                    |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP settings                                           | Gmail SMTP host and port     |

`*.vercel.app` and `*.netlify.app` origins are already allowed by CORS. Railway MySQL variables (`MYSQLHOST`, `MYSQLPORT`, `MYSQLUSER`, `MYSQLPASSWORD`, `MYSQLDATABASE`) are picked up automatically.

### Frontend environment variable

| Variable         | Purpose                                      |
| ---------------- | -------------------------------------------- |
| `VITE_API_URL` | Backend base URL, needed only for deployment |

### Enabling email alerts

```
ALERT_EMAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_gmail_app_password
```

## API Overview

| Method | Endpoint                         | Description                         | Auth   |
| ------ | -------------------------------- | ----------------------------------- | ------ |
| POST   | `/api/auth/register`           | Create an account                   | Public |
| POST   | `/api/auth/login`              | Log in and receive a JWT            | Public |
| GET    | `/api/auth/me`                 | Current user                        | JWT    |
| GET    | `/api/monitors`                | List your monitors                  | JWT    |
| POST   | `/api/monitors`                | Create a monitor                    | JWT    |
| GET    | `/api/monitors/{id}`           | Get one monitor                     | JWT    |
| PUT    | `/api/monitors/{id}`           | Update a monitor                    | JWT    |
| DELETE | `/api/monitors/{id}`           | Delete a monitor and its history    | JWT    |
| POST   | `/api/monitors/{id}/pause`     | Pause a monitor                     | JWT    |
| POST   | `/api/monitors/{id}/resume`    | Resume a monitor                    | JWT    |
| POST   | `/api/monitors/{id}/check`     | Run a check immediately             | JWT    |
| GET    | `/api/monitors/{id}/results`   | Check history (`?hours=24`)       | JWT    |
| GET    | `/api/monitors/{id}/stats`     | Uptime and response-time statistics | JWT    |
| GET    | `/api/monitors/{id}/incidents` | Incidents for a monitor             | JWT    |
| GET    | `/api/dashboard`               | Dashboard summary                   | JWT    |
| GET    | `/api/incidents`               | Recent incidents (`?limit=20`)    | JWT    |
| GET    | `/api/health`                  | Service health                      | Public |
| GET    | `/api/admin/overview`          | System-wide counts                  | ADMIN  |

Full interactive documentation is available in Swagger UI.

## Database Schema

| Table             | Purpose                                               |
| ----------------- | ----------------------------------------------------- |
| `users`         | Accounts, hashed passwords and roles                  |
| `monitors`      | Monitor settings and current status (owned by a user) |
| `check_results` | One row per executed check                            |
| `incidents`     | Downtime periods with start, end and reason           |

Relationships: a user has many monitors, and a monitor has many check results and incidents.

## Deployment

Vercel and Netlify host only the **frontend**. The Spring Boot backend needs a Java host, so the app is deployed in three parts. All three have free tiers.

| Part     | Where                          | How                                                                                          |
| -------- | ------------------------------ | -------------------------------------------------------------------------------------------- |
| MySQL    | Railway, Aiven or Clever Cloud | Create a MySQL service and note the host, user and password                                  |
| Backend  | Render or Railway (Docker)     | Connect the GitHub repo and set the root directory to `backend` (a Dockerfile is included) |
| Frontend | Vercel or Netlify              | Root directory `frontend`, build command `npm run build`, output `dist`                |

**Backend environment variables** (set in the Render or Railway dashboard):

```
DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD   (or a single DB_URL)
JWT_SECRET=<a long random string, 32+ characters>
```

- Tables are created on startup. The `PORT` variable is provided by the host.
- If your cloud MySQL requires SSL (for example, Aiven), set `DB_PARAMS=useSSL=true&serverTimezone=UTC`.
- For a custom frontend domain, add it to `CORS_ORIGINS`.

**Frontend environment variable** (set in the Vercel or Netlify dashboard):

```
VITE_API_URL=https://your-backend.onrender.com
```

`vercel.json` and `netlify.toml` are included so that page refreshes work with client-side routing.

> Note: Render's free tier puts the backend to sleep after about 15 minutes without traffic, which pauses monitoring. For continuous 24/7 monitoring, use an always-on plan or Railway.

## Troubleshooting

| Problem                           | Solution                                                                 |
| --------------------------------- | ------------------------------------------------------------------------ |
| `Access denied for user 'root'` | Your MySQL password differs from the default. Set `DB_PASSWORD`.       |
| `Communications link failure`   | MySQL is not running. Start it (or run `docker compose up -d`).        |
| Port 8080 or 5173 already in use  | Change `server.port` or stop the other process.                        |
| Monitor stays "Pending"           | Wait 5-10 seconds for the first check to finish.                         |
| `npm install` fails on Windows  | Run `npm cache clean --force`, delete `node_modules`, and try again. |

## Roadmap

- Redis caching and rate limiting
- Public status pages (`/status/{slug}`)
- CI/CD with GitHub Actions
- More tests (Mockito for `MonitorCheckProcessor`, `@DataJpaTest`)
- SSRF protection: block private IP ranges (localhost, 10.x, 192.168.x) in production. They are currently allowed to make local testing easy.
