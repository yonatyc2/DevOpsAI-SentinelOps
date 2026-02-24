# How to run SentinelOps AI and verify it works

## Prerequisites

- **Java 17+** and **Maven** installed
- **Node.js 18+** and **npm** installed
- **OpenAI API key** (get one at https://platform.openai.com/api-keys)
- Your **Nedbank servers** are reachable on the network (192.168.174.x) with SSH on port 22

---

## Step 1: Start the backend

Open a terminal in the project folder and run:

```bash
cd backend
set OPENAI_API_KEY=sk-your-actual-openai-key
mvn spring-boot:run
```

*(Replace `sk-your-actual-openai-key` with your real OpenAI API key. On Linux/macOS use `export OPENAI_API_KEY=sk-...`.)*

Wait until you see something like:

```
Started SentinelOpsApplication in X.XXX seconds
```

- On first run, the app loads your 8 Nedbank servers from `backend/data/servers-seed.json` and saves them (encrypted) to `backend/data/servers.json`. The seed file is then renamed to `servers-seed.json.done`.
- Backend runs at **http://localhost:8080**.

---

## Step 2: Start the frontend

Open a **second** terminal and run:

```bash
cd frontend
npm install
npm run dev
```

Wait until you see:

```
VITE ready ... Local: http://localhost:5557/
```

- Frontend runs at **http://localhost:5557** and proxies `/api` to the backend.

---

## Step 3: Open the app in the browser

1. Go to **http://localhost:5557**
2. In the top right, open the **Server** dropdown.
3. You should see **Default (config)** and your 8 Nedbank servers (e.g. nedbank-appserver1, nedbank-configserver, …).
4. Select **nedbank-appserver1** (or any server that is reachable from your machine).

---

## Step 4: Check system state (snapshot)

1. Click **Refresh** next to “System state”.
2. After a few seconds you should see:
   - **Disk** usage bars for each mount (e.g. `/`, `/boot`),
   - **Memory** (RAM and optionally Swap),
   - **Uptime** and load averages.
3. If you see an error message instead (e.g. “Snapshot failed: Connection refused”), the backend cannot SSH to that host. Check:
   - The server is on the same network (192.168.174.x) and reachable (e.g. `ping 192.168.174.2`),
   - SSH is allowed (port 22),
   - Username `equals` and password `123QWEasdZXC` are correct.

---

## Step 5: Example task – run a command

1. Click **Execute command** (top right).
2. In the modal, type: **`df -h`**
3. Click **Analyze risk**. You should see **Risk level: Low**.
4. Click **Approve & run**.
5. After a few seconds you should see:
   - “Command executed (exit code: 0)”,
   - The output of `df -h` (filesystems, size, used, available, use %, mount point).

If you see “SSH not configured or connection failed”, make sure you selected a **server** from the dropdown (not “Default (config)”) and that the server is reachable.

---

## Step 6: Example task – ask the AI (chat)

1. In the main chat input, type: **`What is the disk usage on this server?`**
2. Optionally check **Include system context (full snapshot: Linux, Docker, Postgres)** so the AI can use the latest snapshot.
3. Click **Send**.
4. You should get a short answer from SentinelOps (backed by OpenAI) about disk usage, possibly referring to the snapshot if you included context.

If you see “OpenAI API key is not configured” or “OpenAI request failed”, set `OPENAI_API_KEY` correctly and restart the backend (Step 1).

---

## Quick checklist

| Step | What to do | What you should see |
|------|------------|----------------------|
| 1 | Start backend with `OPENAI_API_KEY` | “Started SentinelOpsApplication” |
| 2 | Start frontend with `npm run dev` | “Local: http://localhost:5557/” |
| 3 | Open http://localhost:5557, pick a server | Server dropdown lists Nedbank servers |
| 4 | Click **Refresh** in System state | Disk, Memory, Uptime or a clear error |
| 5 | Execute command **`df -h`** | Command output in the modal |
| 6 | Ask **“What is the disk usage?”** in chat | AI reply in the chat |

---

## If something fails

- **No servers in dropdown:** Ensure `backend/data/servers-seed.json` exists. If you already ran once, it may have been renamed to `servers-seed.json.done`. To re-import: delete `backend/data/servers.json` and `backend/data/servers-seed.json.done`, rename/copy the seed file back to `servers-seed.json`, then restart the backend.
- **System state / Execute command error:** Select a server from the dropdown (not “Default (config)”) and ensure that server is reachable via SSH (same network, port 22, correct credentials).
- **Chat returns an error about OpenAI:** Set `OPENAI_API_KEY` and restart the backend.
