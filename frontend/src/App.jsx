import { useState, useRef, useEffect } from 'react'
import './App.css'

const API_BASE = '/api'

export default function App() {
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [includeContext, setIncludeContext] = useState(false)
  const [snapshot, setSnapshot] = useState(null)
  const [snapshotLoading, setSnapshotLoading] = useState(false)
  const [snapshotError, setSnapshotError] = useState(null)
  const [commandModalOpen, setCommandModalOpen] = useState(false)
  const [commandInput, setCommandInput] = useState('')
  const [commandAnalysis, setCommandAnalysis] = useState(null)
  const [commandExecuteResult, setCommandExecuteResult] = useState(null)
  const [commandLoading, setCommandLoading] = useState(false)
  const [servers, setServers] = useState([])
  const [selectedServerId, setSelectedServerId] = useState('')
  const [analytics, setAnalytics] = useState({ disk: null, anomalies: [] })
  const [chatMode, setChatMode] = useState('UNKNOWN')
  const messagesEndRef = useRef(null)

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  const fetchServers = async () => {
    try {
      const res = await fetch(`${API_BASE}/servers`)
      if (res.ok) {
        const data = await res.json()
        setServers(data || [])
      }
    } catch (_) {}
  }

  const fetchChatMode = async () => {
    try {
      const res = await fetch(`${API_BASE}/chat/mode`)
      if (!res.ok) {
        setChatMode('UNKNOWN')
        return
      }
      const data = await safeJson(res)
      setChatMode(data.mode || 'UNKNOWN')
    } catch (_) {
      setChatMode('UNKNOWN')
    }
  }

  const safeJson = async (res) => {
    const text = await res.text()
    if (!text.trim()) return {}
    try {
      return JSON.parse(text)
    } catch {
      return { error: res.statusText || 'Invalid response' }
    }
  }

  const fetchSnapshot = async () => {
    setSnapshotLoading(true)
    setSnapshotError(null)
    try {
      const url = selectedServerId ? `${API_BASE}/snapshot?serverId=${encodeURIComponent(selectedServerId)}` : `${API_BASE}/snapshot`
      const res = await fetch(url)
      const data = await safeJson(res)
      if (!res.ok) {
        setSnapshotError(data.error || res.statusText || 'Request failed')
        setSnapshot(null)
        return
      }
      setSnapshot(data)
    } catch (err) {
      setSnapshotError(err.message)
      setSnapshot(null)
    } finally {
      setSnapshotLoading(false)
    }
  }

  const fetchAnalytics = async () => {
    if (!selectedServerId) return
    try {
      const [diskRes, anomaliesRes] = await Promise.all([
        fetch(`${API_BASE}/analytics/disk?serverId=${encodeURIComponent(selectedServerId)}&limit=30`),
        fetch(`${API_BASE}/analytics/anomalies?serverId=${encodeURIComponent(selectedServerId)}&lastN=20`),
      ])
      const disk = diskRes.ok ? await diskRes.json() : null
      const anomalies = anomaliesRes.ok ? await anomaliesRes.json() : []
      setAnalytics({ disk, anomalies })
    } catch (_) {
      setAnalytics({ disk: null, anomalies: [] })
    }
  }

  useEffect(() => {
    fetchServers()
    fetchChatMode()
  }, [])

  useEffect(() => {
    fetchSnapshot()
  }, [selectedServerId])

  useEffect(() => {
    if (selectedServerId) {
      fetch(`${API_BASE}/servers/${selectedServerId}/health`).then(() => fetchServers())
    }
  }, [selectedServerId])

  useEffect(() => {
    if (snapshot && selectedServerId) fetchAnalytics()
  }, [snapshot, selectedServerId])

  const sendMessage = async () => {
    const text = input.trim()
    if (!text || loading) return

    const userMessage = { role: 'user', content: text }
    setMessages((prev) => [...prev, userMessage])
    setInput('')
    setLoading(true)

    try {
      const res = await fetch(`${API_BASE}/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          message: text,
          includeSystemContext: includeContext,
          serverId: selectedServerId || null,
        }),
      })
      const data = await safeJson(res)
      const content = !res.ok ? (data.error || res.statusText || 'Request failed') : (data.response ?? 'No response received.')
      if (data.mode) setChatMode(data.mode)
      setMessages((prev) => [...prev, { role: 'assistant', content }])
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: `Error: ${err.message}. Is the backend running on port 8080?` },
      ])
    } finally {
      setLoading(false)
    }
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      sendMessage()
    }
  }

  const openCommandModal = () => {
    setCommandModalOpen(true)
    setCommandInput('')
    setCommandAnalysis(null)
    setCommandExecuteResult(null)
  }

  const closeCommandModal = () => {
    setCommandModalOpen(false)
    setCommandInput('')
    setCommandAnalysis(null)
    setCommandExecuteResult(null)
  }

  const analyzeCommand = async () => {
    const cmd = commandInput.trim()
    if (!cmd || commandLoading) return
    setCommandLoading(true)
    setCommandExecuteResult(null)
    try {
      const res = await fetch(`${API_BASE}/commands/analyze`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ command: cmd }),
      })
      const data = await safeJson(res)
      if (!res.ok) {
        setCommandAnalysis({ riskLevel: 'LOW', reason: 'Analysis failed: ' + (data.error || res.statusText), rollbackSuggestion: '' })
      } else {
        setCommandAnalysis(data)
      }
    } catch (err) {
      setCommandAnalysis({ riskLevel: 'LOW', reason: 'Analysis failed: ' + err.message, rollbackSuggestion: '' })
    } finally {
      setCommandLoading(false)
    }
  }

  const executeCommand = async () => {
    const cmd = commandInput.trim()
    if (!cmd || !commandAnalysis || commandLoading) return
    setCommandLoading(true)
    try {
      const res = await fetch(`${API_BASE}/commands/execute`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          command: cmd,
          confirmedRiskLevel: commandAnalysis.riskLevel,
          serverId: selectedServerId || null,
        }),
      })
      const data = await safeJson(res)
      if (!res.ok) {
        setCommandExecuteResult({ executed: false, rejectionReason: data.error || res.statusText || 'Request failed' })
      } else {
        setCommandExecuteResult(data)
      }
    } catch (err) {
      setCommandExecuteResult({ executed: false, rejectionReason: err.message })
    } finally {
      setCommandLoading(false)
    }
  }

  const riskLabel = (level) => {
    if (!level) return ''
    const s = String(level)
    return s.charAt(0) + s.slice(1).toLowerCase()
  }

  const mbToGb = (mb) => {
    const val = Number(mb || 0)
    return (val / 1024).toFixed(2)
  }

  const parseSizeToGb = (value) => {
    if (!value) return null
    const text = String(value).trim()
    const match = text.match(/^([0-9]*\.?[0-9]+)\s*([kKmMgGtTpP]?)i?[bB]?$/)
    if (!match) return null
    const num = Number(match[1])
    if (!Number.isFinite(num)) return null
    const unit = (match[2] || '').toUpperCase()
    const factors = { '': 1 / (1024 ** 3), K: 1 / (1024 ** 2), M: 1 / 1024, G: 1, T: 1024, P: 1024 ** 2 }
    const factor = Object.prototype.hasOwnProperty.call(factors, unit) ? factors[unit] : null
    if (factor == null) return null
    return num * factor
  }

  const formatDiskGb = (value) => {
    const gb = parseSizeToGb(value)
    if (gb == null) return 'N/A'
    return `${gb.toFixed(2)} GB`
  }

  return (
    <div className="app">
      <header className="header">
        <div className="header-top">
          <div>
            <h1>SentinelOps AI</h1>
            <p className="tagline">DevOps assistant for Linux, Docker & PostgreSQL</p>
            <div className={`mode-badge mode-badge--${String(chatMode).toLowerCase()}`}>
              {chatMode === 'OPENAI' ? 'OpenAI mode' : chatMode === 'LOCAL' ? 'Local mode' : 'Mode unknown'}
            </div>
          </div>
          <div className="header-actions">
            <div className="server-select-wrap">
              <label className="server-label">Server</label>
              <select
                className="server-select"
                value={selectedServerId}
                onChange={(e) => setSelectedServerId(e.target.value)}
              >
                <option value="">Default (config)</option>
                {servers.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name || s.host} {s.health ? `(${s.health})` : ''}
                  </option>
                ))}
              </select>
            </div>
            <button type="button" className="execute-cmd-btn" onClick={openCommandModal}>
              Execute command
            </button>
          </div>
        </div>
      </header>

      <div className="system-panel">
        <div className="system-panel-header">
          <h2>System state</h2>
          <button type="button" className="refresh-btn" onClick={fetchSnapshot} disabled={snapshotLoading}>
            {snapshotLoading ? 'Refreshing…' : 'Refresh'}
          </button>
        </div>
        {snapshotError && <p className="system-panel-error">{snapshotError}</p>}
        {snapshot && !snapshotError && (
          <div className="system-panel-content">
            {snapshot.linux && (
              <>
                {snapshot.linux.diskUsage?.length > 0 && (
                  <div className="panel-section">
                    <h3>Disk</h3>
                    <div className="disk-header">
                      <span className="disk-header-mount">Mount</span>
                      <span className="disk-header-size">Used / Total</span>
                    </div>
                    {snapshot.linux.diskUsage.map((d, i) => {
                      const pct = parseInt(String(d.usePercent).replace('%', ''), 10) || 0
                      const isHigh = pct >= 90
                      const isWarn = pct >= 75
                      return (
                        <div key={i} className="disk-row">
                          <span className="disk-mount" title={d.filesystem}>{d.mountedOn || d.filesystem}</span>
                          <div className="disk-bar-wrap">
                            <div
                              className={`disk-bar ${isHigh ? 'disk-bar--high' : isWarn ? 'disk-bar--warn' : ''}`}
                              style={{ width: `${Math.min(pct, 100)}%` }}
                            />
                          </div>
                          <span className="disk-pct">{d.usePercent}</span>
                          <span className="disk-size-gb">{formatDiskGb(d.used)} / {formatDiskGb(d.size)}</span>
                        </div>
                      )
                    })}
                  </div>
                )}
                {snapshot.linux.memory && (
                  <div className="panel-section">
                    <h3>Memory</h3>
                    <div className="memory-row">
                      <span className="memory-label">RAM</span>
                      <div className="disk-bar-wrap">
                        <div
                          className="disk-bar memory-bar"
                          style={{
                            width: `${snapshot.linux.memory.memTotalMb
                              ? Math.min(100, (100 * snapshot.linux.memory.memUsedMb) / snapshot.linux.memory.memTotalMb)
                              : 0}%`,
                          }}
                        />
                      </div>
                      <span className="memory-value">
                        {mbToGb(snapshot.linux.memory.memUsedMb)} / {mbToGb(snapshot.linux.memory.memTotalMb)} GB
                      </span>
                    </div>
                    {snapshot.linux.memory.swapTotalMb > 0 && (
                      <div className="memory-row">
                        <span className="memory-label">Swap</span>
                        <div className="disk-bar-wrap">
                          <div
                            className="disk-bar memory-bar memory-bar--swap"
                            style={{
                              width: `${(100 * snapshot.linux.memory.swapUsedMb) / snapshot.linux.memory.swapTotalMb}%`,
                            }}
                          />
                        </div>
                        <span className="memory-value">
                          {mbToGb(snapshot.linux.memory.swapUsedMb)} / {mbToGb(snapshot.linux.memory.swapTotalMb)} GB
                        </span>
                      </div>
                    )}
                  </div>
                )}
                {snapshot.linux.cpuUsagePercent != null && (
                  <div className="panel-section">
                    <h3>CPU</h3>
                    {(() => {
                      const cpuPct = Number(snapshot.linux.cpuUsagePercent || 0)
                      const cpuIsHigh = cpuPct >= 90
                      const cpuIsWarn = cpuPct >= 75
                      return (
                        <div className="memory-row">
                          <span className="memory-label">CPU</span>
                          <div className="disk-bar-wrap">
                            <div
                              className={`disk-bar cpu-bar ${cpuIsHigh ? 'cpu-bar--high' : cpuIsWarn ? 'cpu-bar--warn' : ''}`}
                              style={{ width: `${Math.min(100, Math.max(0, cpuPct))}%` }}
                            />
                          </div>
                          <span className="memory-value">{cpuPct.toFixed(1)}%</span>
                        </div>
                      )
                    })()}
                  </div>
                )}
                {snapshot.linux.uptime?.uptimeString && (
                  <div className="panel-section">
                    <h3>Uptime</h3>
                    <p className="uptime-text mono">{snapshot.linux.uptime.uptimeString}</p>
                    {(snapshot.linux.uptime.load1 != null) && (
                      <p className="load-text">Load: {snapshot.linux.uptime.load1}, {snapshot.linux.uptime.load5}, {snapshot.linux.uptime.load15}</p>
                    )}
                  </div>
                )}
                {snapshot.linux.error && <p className="panel-error">{snapshot.linux.error}</p>}
              </>
            )}
            {snapshot.docker?.containers?.length > 0 && (
              <div className="panel-section">
                <h3>Docker</h3>
                <ul className="container-list">
                  {snapshot.docker.containers.map((c, i) => (
                    <li key={i} className={`container-item container-item--${(c.state || '').toLowerCase()}`}>
                      <span className="container-name">{c.name}</span>
                      <span className="container-state">{c.state}</span>
                      {c.uptime && <span className="container-uptime" title="Container uptime">{c.uptime}</span>}
                      {c.restartCount > 0 && <span className="container-restarts" title="Restart count">{c.restartCount} restarts</span>}
                    </li>
                  ))}
                </ul>
              </div>
            )}
            {snapshot.postgres && (snapshot.postgres.activeConnections !== undefined || snapshot.postgres.databaseSizes?.length > 0) && (
              <div className="panel-section">
                <h3>PostgreSQL</h3>
                {snapshot.postgres.activeConnections !== undefined && (
                  <p>Active connections: {snapshot.postgres.activeConnections}</p>
                )}
                {snapshot.postgres.databaseSizes?.length > 0 && (
                  <ul className="db-list">
                    {snapshot.postgres.databaseSizes.slice(0, 3).map((db, i) => (
                      <li key={i} className="mono">{db.name}: {db.size}</li>
                    ))}
                  </ul>
                )}
              </div>
            )}
            {!snapshot.linux?.diskUsage?.length && !snapshot.linux?.memory?.memTotalMb && !snapshot.docker?.containers?.length && !snapshot.postgres?.activeConnections && !snapshot.linux?.error && (
              <p className="system-panel-muted">No snapshot data (SSH may be unconfigured).</p>
            )}
          </div>
        )}
      </div>

      {selectedServerId && (analytics.disk?.byMount || analytics.anomalies?.length > 0) && (
        <div className="analytics-panel">
          <h2 className="analytics-title">Analytics &amp; anomalies</h2>
          {analytics.anomalies?.length > 0 && (
            <div className="panel-section">
              <h3>Detected</h3>
              <ul className="anomalies-list">
                {analytics.anomalies.map((a, i) => (
                  <li key={i} className={`anomaly anomaly--${(a.severity || '').toLowerCase()}`}>
                    <span className="anomaly-type">{a.type}</span> {a.message}
                  </li>
                ))}
              </ul>
            </div>
          )}
          {analytics.disk?.byMount && Object.keys(analytics.disk.byMount).length > 0 && (
            <div className="panel-section disk-trend">
              <h3>Disk trend</h3>
              {Object.entries(analytics.disk.byMount).map(([mount, points]) => (
                <div key={mount} className="disk-trend-row">
                  <span className="disk-trend-mount">{mount}</span>
                  <div className="disk-trend-bars">
                    {(points.slice(-15) || []).map((p, j) => (
                      <div
                        key={j}
                        className="disk-trend-bar"
                        style={{
                          height: `${Math.min(100, p.usePercent || 0)}%`,
                          backgroundColor: (p.usePercent || 0) >= 90 ? 'var(--danger)' : (p.usePercent || 0) >= 75 ? 'var(--warning)' : 'var(--success)',
                        }}
                        title={`${p.timestamp}: ${p.usePercent}%`}
                      />
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      <main className="main">
        <div className="messages">
          {messages.length === 0 && (
            <div className="welcome">
              <p>Ask anything about your infrastructure.</p>
              <p className="examples">
                e.g. “Why is /data almost full?”, “Why is Docker restarting?”, “Can I safely remove sdb1?”
              </p>
            </div>
          )}
          {messages.map((msg, i) => (
            <div key={i} className={`message message--${msg.role}`}>
              <span className="message-role">{msg.role === 'user' ? 'You' : 'SentinelOps'}</span>
              <div className="message-content">{msg.content}</div>
            </div>
          ))}
          {loading && (
            <div className="message message--assistant">
              <span className="message-role">SentinelOps</span>
              <div className="message-content typing">Thinking…</div>
            </div>
          )}
          <div ref={messagesEndRef} />
        </div>

        <div className="input-area">
          <label className="checkbox">
            <input
              type="checkbox"
              checked={includeContext}
              onChange={(e) => setIncludeContext(e.target.checked)}
            />
            Include system context (full snapshot: Linux, Docker, Postgres)
          </label>
          <div className="input-row">
            <textarea
              className="input"
              placeholder="Ask a question…"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              rows={1}
              disabled={loading}
            />
            <button
              className="send-btn"
              onClick={sendMessage}
              disabled={loading || !input.trim()}
            >
              Send
            </button>
          </div>
        </div>
      </main>

      {commandModalOpen && (
        <div className="modal-overlay" onClick={closeCommandModal}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Approve command</h2>
              <button type="button" className="modal-close" onClick={closeCommandModal} aria-label="Close">&times;</button>
            </div>
            <div className="modal-body">
              <label className="modal-label">Command to run on server</label>
              <textarea
                className="modal-input mono"
                placeholder="e.g. df -h"
                value={commandInput}
                onChange={(e) => setCommandInput(e.target.value)}
                rows={2}
                disabled={commandLoading}
              />
              {!commandAnalysis ? (
                <button type="button" className="modal-btn modal-btn--primary" onClick={analyzeCommand} disabled={commandLoading || !commandInput.trim()}>
                  {commandLoading ? 'Analyzing…' : 'Analyze risk'}
                </button>
              ) : (
                <>
                  <div className="risk-section">
                    <span className="modal-label">Risk level</span>
                    <span className={`risk-badge risk-badge--${(commandAnalysis.riskLevel || '').toLowerCase()}`}>
                      {riskLabel(commandAnalysis.riskLevel)}
                    </span>
                  </div>
                  <p className="risk-reason">{commandAnalysis.reason}</p>
                  {commandAnalysis.rollbackSuggestion && (
                    <p className="risk-rollback"><strong>Rollback:</strong> {commandAnalysis.rollbackSuggestion}</p>
                  )}
                  {commandExecuteResult ? (
                    <div className="execute-result">
                      {commandExecuteResult.executed ? (
                        <>
                          <p className="execute-status execute-status--ok">Command executed (exit code: {commandExecuteResult.exitCode})</p>
                          {commandExecuteResult.stdout && <pre className="execute-output">{commandExecuteResult.stdout}</pre>}
                          {commandExecuteResult.stderr && <pre className="execute-output execute-output--err">{commandExecuteResult.stderr}</pre>}
                          {commandExecuteResult.rollbackSuggestion && (
                            <p className="risk-rollback"><strong>Rollback:</strong> {commandExecuteResult.rollbackSuggestion}</p>
                          )}
                        </>
                      ) : (
                        <p className="execute-status execute-status--fail">{commandExecuteResult.rejectionReason}</p>
                      )}
                    </div>
                  ) : (
                    <div className="modal-actions">
                      <button type="button" className="modal-btn modal-btn--primary" onClick={executeCommand} disabled={commandLoading}>
                        {commandLoading ? 'Running…' : 'Approve & run'}
                      </button>
                      <button type="button" className="modal-btn modal-btn--secondary" onClick={() => { setCommandAnalysis(null); setCommandExecuteResult(null); }} disabled={commandLoading}>
                        Back
                      </button>
                    </div>
                  )}
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
