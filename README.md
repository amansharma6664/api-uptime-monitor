# Uptime Monitor (UptimeRobot jaisa API Monitoring Tool)

Full stack project: **Java 17 + Spring Boot 3 + Maven + MySQL + React (Vite)**.
User apne websites / API URLs add karta hai. Backend har X second me unhe ping karta hai,
response time aur status MySQL me save karta hai, website down hone par incident banata hai
aur alert (email / Slack / Discord webhook) bhejta hai. React dashboard me live status aur charts dikhte hain.

---

## 1. Kya-kya chahiye (Prerequisites)

| Cheez | Version |
|---|---|
| JDK | 17 ya usse upar |
| Maven | 3.8+ |
| Node.js | 18+ |
| MySQL | 8.x (running, port 3306) |

MySQL install nahi hai? Docker hai to: `docker compose up -d` (root folder me). Ye root/root ke saath MySQL chala dega.

## 2. Run kaise kare (2 terminals)

**Terminal 1 - Backend** (port 8080)
```
cd backend
mvn spring-boot:run
```
(ya root folder se `start-backend.bat` / `./start-backend.sh`)

**Terminal 2 - Frontend** (port 5173)
```
cd frontend
npm install
npm run dev
```
(ya `start-frontend.bat` / `./start-frontend.sh`)

Phir browser me kholo: **http://localhost:5173**

Koi demo / hardcoded user nahi hai. **Register page** par apna name, email, password daalke account banao
aur phir usi se login karo. Sabse pehla registered user automatically ADMIN banta hai.

### Database ke baare me (ek hi cheez check karni hai)
- Database `uptime_monitor` aur saari tables **khud ban jati hain**, kuch manually nahi banana.
- Default MySQL login: **username `root`, password `root`**, host `localhost:3306`.
- Tumhara MySQL password alag hai? Ya to environment variable set karo:
  - Windows (cmd): `set DB_PASSWORD=tumhara_password` phir `mvn spring-boot:run`
  - Mac/Linux: `DB_PASSWORD=tumhara_password mvn spring-boot:run`
  - ya `backend/src/main/resources/application.properties` me `spring.datasource.password` badal do.
- Poori list: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`.

### Useful links
- Swagger API docs: http://localhost:8080/swagger-ui.html
- Health endpoint: http://localhost:8080/api/health

## 3. Features

- JWT login / register (Spring Security, BCrypt, roles: USER / ADMIN)
- Monitor CRUD: name, URL, GET/HEAD, interval (10s - 1h), timeout, expected status code
- Pause / Resume / "Check now"
- **Scheduler + thread pool**: har 5 sec me due monitors dhundh ke parallel checks chalte hain
- Response time + status code + error message har check ka save hota hai
- **Incident tracking**: lagatar 2 failures par DOWN + incident open, recover hone par incident close + downtime duration
- **Alerts**: console log, email (optional SMTP), Slack/Discord webhook (per monitor)
- Uptime % (24h / 7d / 30d), average response time, dashboard summary
- Response time chart (Recharts) with 1h / 24h / 7d / 30d range, DOWN points red me
- Roz raat 3 baje purana data cleanup (30 din retention)
- Global exception handling, validation, Swagger docs, unit tests (JUnit)

### Email alerts on karna (optional)
`application.properties` me ya environment variables se:
```
ALERT_EMAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=tumhari_email@gmail.com
MAIL_PASSWORD=gmail_app_password
```

## 4. Project structure

```
uptime-monitor/
├── backend/                    Spring Boot (Maven)
│   └── src/main/java/com/aman/uptime/
│       ├── config/             Security, CORS, thread pool
│       ├── security/           JWT service + filter
│       ├── entity/             User, Monitor, CheckResult, Incident
│       ├── repository/         Spring Data JPA repositories
│       ├── dto/                Request / response records
│       ├── service/            Business logic (checks, alerts, stats)
│       ├── scheduler/          MonitorScheduler (@Scheduled)
│       ├── controller/         REST controllers
│       └── exception/          Global error handler
├── frontend/                   React + Vite
│   └── src/ pages/ components/ api.js auth.jsx
├── database/schema.sql         Optional manual MySQL setup
├── docker-compose.yml          Optional local MySQL
└── README.md
```

## 5. Architecture (interview me ye explain karo)

```
React  --JWT-->  REST API (Spring Security)  --> MySQL
                      |
        MonitorScheduler (@Scheduled, har 5s)
                      |  due monitors dhundhta hai
                      v
        ExecutorService (10 threads)  --> HttpCheckService (java.net.http.HttpClient)
                      |
                      v
        MonitorCheckProcessor:  result save -> status update -> incident open/close -> AlertService
```

Important design decisions:
1. **HTTP call transaction ke bahar** - slow website ki wajah se DB connection block nahi hota.
2. **`nextCheckAt` pehle update** - scheduler dubara wahi monitor dispatch nahi karta, plus `inFlight` set se overlap nahi hota.
3. **Failure threshold (2)** - ek temporary glitch par false alarm nahi aata (flapping se bachav).
4. **Alerts commit ke baad** - DB transaction ke andar email bhejna avoid kiya.
5. **Chart downsampling** - 300 se zyada points ho to kam kiye jate hain, par failed checks hamesha rakhe jate hain.
6. **Ownership check** - har monitor query me owner id check hota hai, doosre user ka data access nahi hota.

## 6. Resume ke liye

**Project title:** API Uptime & Monitoring Platform | Java, Spring Boot, MySQL, React

- Built a full-stack uptime monitoring platform (UptimeRobot-style) with Spring Boot, MySQL and React; users monitor HTTP endpoints at configurable intervals and view live status, uptime % and response-time charts.
- Designed a scheduler-driven engine (`@Scheduled` + fixed thread pool) that runs health checks in parallel without blocking DB transactions, with overlap protection for in-flight checks.
- Implemented incident tracking with a consecutive-failure threshold to avoid false alerts, plus email and Slack/Discord webhook notifications on DOWN and recovery.
- Secured the REST API with JWT and role-based access (Spring Security), added validation, centralized exception handling, Swagger docs and JUnit tests.

Resume me numbers jodna ho to khud test karke likho (jaise "monitors N endpoints every 30s"), jo tumne actually chalaya ho wahi likhna.

## 7. Agla step (aur impressive banane ke liye)

- Redis caching / rate limiting
- Public status page (`/status/{slug}`)
- Dockerfile for backend + frontend, CI/CD with GitHub Actions
- More tests (Mockito for `MonitorCheckProcessor`, `@DataJpaTest`)
- SSRF protection: production me private IP ranges (localhost, 10.x, 192.168.x) block karo (abhi local demo ke liye allowed hain)

## 8. Troubleshooting

| Problem | Solution |
|---|---|
| `Access denied for user 'root'` | MySQL password alag hai, `DB_PASSWORD` set karo (upar dekho) |
| `Communications link failure` | MySQL running nahi hai, start karo (ya `docker compose up -d`) |
| Port 8080 / 5173 busy | `server.port` badlo ya doosra process band karo |
| Monitor "Pending" dikhe | 5-10 second ruko, pehla check abhi chal raha hai |

## 9. Live deploy (Vercel / Netlify + backend + MySQL)

**Important:** Vercel aur Netlify sirf *frontend* (React) host karte hain. Spring Boot backend ko Java server chahiye,
isliye 3 hisse alag deploy hote hain (teeno ka free tier hai):

| Hissa | Kahan | Kaise |
|---|---|---|
| MySQL | Railway / Aiven / Clever Cloud | MySQL service banao, uski host/user/password details note karo |
| Backend | Render / Railway (Docker) | GitHub repo connect karo, root directory = `backend` (Dockerfile ready hai) |
| Frontend | Vercel / Netlify | root directory = `frontend`, build command `npm run build`, output `dist` |

**Backend ke environment variables** (Render/Railway dashboard me):
```
DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD     (ya sirf DB_URL = poora jdbc:mysql://... url)
JWT_SECRET = koi bhi lamba random string (32+ characters)
```
- Railway par MySQL isi project me banaya to `MYSQLHOST` jaise variables apne aap pad liye jate hain.
- Database tables startup par khud ban jati hain. `PORT` cloud khud deta hai.
- Cloud MySQL SSL maangta ho (jaise Aiven) to `DB_PARAMS=useSSL=true&serverTimezone=UTC` set karo.
- CORS me `*.vercel.app` aur `*.netlify.app` pehle se allowed hain, kuch badalna nahi. Custom domain ho to `CORS_ORIGINS` me daalo.

**Frontend ka ek hi environment variable** (Vercel/Netlify dashboard -> Environment Variables):
```
VITE_API_URL = https://tumhara-backend.onrender.com
```
Ye kisi file me edit nahi karna, bas dashboard me daalna hai (backend ka URL deploy ke baad hi pata chalta hai, isliye
code me pehle se likhna possible nahi). `vercel.json` / `netlify.toml` (page refresh par routing) already included hain.

Free tier note: Render ka free backend 15 min idle rehne par sleep ho jata hai, to us dauran monitoring ruk jati hai.
Real 24x7 monitoring ke liye always-on plan ya Railway use karo.
