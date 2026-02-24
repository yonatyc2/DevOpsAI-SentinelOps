# SentinelOps AI

AI DevOps Assistant for Linux, Docker & PostgreSQL.

## What's included

### Phase 1 — Core Infrastructure
- **Backend:** Spring Boot, `/api/chat`, SSH execution (JSch), OpenAI integration, basic prompt.
- **Frontend:** React (Vite) chat UI, “Include system context” option, display AI responses.

### Phase 2 — System Snapshot Engine
- **Backend:** Structured snapshot collection and JSON models:
  - **LinuxSnapshotService:** `df -h`, `free -m`, `uptime` → disk usage, memory, load.
  - **DockerSnapshotService:** `docker ps`, `docker stats`, restart counts via `docker inspect`.
  - **PostgresSnapshotService:** active connections, database sizes, locks (via `psql` on SSH host).
- **API:** `GET /api/snapshot` returns full `SystemSnapshot` (Linux + Docker + Postgres).
- **Chat:** When “Include system context” is checked, the AI receives the full snapshot as JSON.
- **Frontend:** System state panel with disk usage bars, memory (RAM/Swap) indicators, uptime/load, Docker container list (state, restart count), and optional Postgres summary.

### Phase 3 — Safety Engine
- **Backend:** Command risk analysis and confirmation workflow:
  - **CommandRiskAnalyzer:** Classifies commands as Low / Medium / High risk (e.g. `rm -rf`, `lvreduce`, `drop database`, `systemctl stop`, `docker rm -f`), with reasons and rollback suggestions.
  - **Commands API:** `POST /api/commands/analyze` (risk + rollback), `POST /api/commands/execute` (requires matching `confirmedRiskLevel` for medium/high), `GET /api/commands/history` (audit log).
  - **CommandHistoryService:** In-memory log of executed commands (timestamp, command, risk level, exit code, stdout/stderr, rollback suggestion).
- **Frontend:** **Execute command** button opens an **Approve command** modal: enter command → **Analyze risk** → see **risk level** (Low/Medium/High badge), reason, and rollback hint → **Approve & run** or Cancel. Execution result (stdout/stderr or rejection) and rollback suggestion shown in the modal.

### Phase 4 — Multi-Server Support
- **Backend:** Multiple servers with stored SSH credentials.
  - **Server** entity: name, host, port, username, auth type (PASSWORD or PRIVATE_KEY), encrypted credential (AES-GCM). Persisted to `data/servers.json`.
  - **Servers API:** `GET /api/servers`, `POST /api/servers` (create), `PUT /api/servers/:id`, `DELETE /api/servers/:id`, `GET /api/servers/:id/health` (runs `echo ok` via SSH, updates server health).
  - Snapshot, chat (with context), and command execute accept optional `serverId`; SSH runs against the selected server or default config.
- **Frontend:** **Server** dropdown (Default or stored servers). Per-server health shown in the list; health is refreshed when a server is selected. Snapshot, chat context, and Execute command use the selected server.

### Phase 5 — Intelligence & Analytics
- **Backend:** Historical snapshots and anomaly detection.
  - **SnapshotHistoryService:** Keeps last 200 snapshots in memory; each snapshot is stored with `serverId` when captured via `GET /api/snapshot`.
  - **Analytics API:** `GET /api/analytics/disk?serverId=&limit=50` (disk use % per mount over time), `GET /api/analytics/memory?serverId=&limit=50`, `GET /api/analytics/anomalies?serverId=&lastN=20` (detected issues).
  - **AnomalyDetectionService:** Compares last two snapshots: disk growth (e.g. mount &gt;90% or +10% growth), container restart count &gt;3 (restart loop), memory increase ≥5% (possible leak).
- **Frontend:** When a server is selected, **Analytics & anomalies** panel shows detected anomalies (type + message) and a **Disk trend** mini-chart (last 15 points per mount, color by use %).

## Prerequisites

- **Java 17+** and **Maven** (backend)
- **Node.js 18+** and **npm** (frontend)
- **OpenAI API key** (required for chat)
- Optional: **SSH** access to a Linux host (for snapshot and context)
- Optional: **PostgreSQL** (enable via `POSTGRES_ENABLED=true` and connection settings)

## Quick start

### 1. Backend

```bash
cd backend
set OPENAI_API_KEY=sk-your-key   # Windows
# export OPENAI_API_KEY=sk-your-key   # Linux/macOS

mvn spring-boot:run
```

Backend runs at **http://localhost:8080**.

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at **http://localhost:5557** and proxies `/api` to the backend.

### 3. Use the app

1. Open http://localhost:5557  
2. **System state** panel loads from `GET /api/snapshot` (refresh with the button).  
3. Ask questions in the chat; check **Include system context** to send the full snapshot (Linux + Docker + Postgres) to the AI.  
4. AI answers using the structured snapshot when context is included.
5. Use **Execute command** to run a command on the server: analyze risk, then approve and run (medium/high risk requires confirming the shown level).
6. **Server** dropdown: choose Default (SSH from config) or a stored server. You can add servers via API, or **seed on startup**: place a `backend/data/servers-seed.json` file (array of `{ "name", "host", "port", "username", "password" }`). On first run the app imports them (encrypts credentials), then renames the file to `.done` so it is not loaded again.
7. After refreshing snapshot a few times for a server, **Analytics & anomalies** shows disk trend and any detected issues (disk growth, restart loops, memory trend).

## Configuration (backend)

| Key | Description | Default |
|-----|-------------|---------|
| `OPENAI_API_KEY` | OpenAI API key | (required) |
| `openai.model` | Model name | `gpt-4o-mini` |
| `ssh.host`, `ssh.port` | SSH host for snapshot/context | `localhost`, `22` |
| `ssh.username` | SSH user | `root` |
| `SSH_PASSWORD` or `SSH_KEY_PATH` | SSH auth | (optional) |
| `POSTGRES_ENABLED` | Enable Postgres snapshot (psql via SSH) | `false` |
| `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_USER`, `POSTGRES_DATABASE` | Postgres connection (on SSH host) | `localhost`, `5432`, `postgres`, `postgres` |
| `ENCRYPTION_SECRET` | Secret for encrypting stored server credentials (Phase 4) | (default in app) |

## Testing (end-to-end)

Backend integration tests hit real HTTP endpoints with a mocked OpenAI service (no API key needed):

```bash
cd backend
mvn test
```

Tests cover:
- **Chat:** `POST /api/chat` with valid message returns 200 and AI response; empty/blank message returns 400.
- **Snapshot:** `GET /api/snapshot` returns 200 and JSON with `linux`, `docker`, `postgres`, `timestamp`.
- **Commands:** `POST /api/commands/analyze` returns risk level; `POST /api/commands/execute` runs command (or rejects); `GET /api/commands/history` returns array.

To verify the full stack: run `mvn test` in `backend`, then `npm run build` in `frontend`.

## Project layout

```
backend/
  src/main/java/com/sentinelops/
    config/           # SshProperties, OpenAiProperties, PostgresProperties, EncryptionProperties
    controller/       # ChatController, SnapshotController, CommandsController, ServersController, AnalyticsController
    model/            # Server, RiskLevel, CommandRiskResult, CommandLogEntry, Anomaly, SnapshotHistoryEntry; snapshot/ ...
    repository/       # ServerRepository (file-backed)
    service/          # SshExecutionService, OpenAiService, ChatService, CredentialEncryptionService,
                      # CommandRiskAnalyzer, CommandExecutionService, CommandHistoryService,
                      # LinuxSnapshotService, DockerSnapshotService, PostgresSnapshotService,
                      # SnapshotAggregatorService, SnapshotHistoryService, AnomalyDetectionService
  data/               # servers.json (created at runtime)
frontend/
  src/
    App.jsx, App.css  # Chat UI + system panel + Approve command modal + server dropdown + analytics
    main.jsx, index.css
```
