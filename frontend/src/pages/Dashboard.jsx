import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage } from '../api';
import MonitorForm from '../components/MonitorForm.jsx';
import StatusBadge from '../components/StatusBadge.jsx';
import { formatMs, formatPct, timeAgo } from '../utils';

const REFRESH_MS = 10000;

export default function Dashboard() {
  const [summary, setSummary] = useState(null);
  const [monitors, setMonitors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showForm, setShowForm] = useState(false);

  const load = useCallback(async () => {
    try {
      const [s, m] = await Promise.all([api.get('/dashboard'), api.get('/monitors')]);
      setSummary(s.data);
      setMonitors(m.data);
      setError('');
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
    const timer = setInterval(load, REFRESH_MS);
    return () => clearInterval(timer);
  }, [load]);

  const togglePause = async (m) => {
    try {
      await api.post(`/monitors/${m.id}/${m.active ? 'pause' : 'resume'}`);
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  const remove = async (m) => {
    if (!window.confirm(`Delete monitor "${m.name}"? All its history will be removed.`)) return;
    try {
      await api.delete(`/monitors/${m.id}`);
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Monitors</h1>
          <p className="muted">Auto-refreshes every {REFRESH_MS / 1000} seconds</p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowForm(true)}>+ Add monitor</button>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {summary && (
        <div className="stat-grid">
          <div className="stat-card"><div className="stat-label">Total</div><div className="stat-value">{summary.totalMonitors}</div></div>
          <div className="stat-card"><div className="stat-label">Up</div><div className="stat-value text-up">{summary.up}</div></div>
          <div className="stat-card"><div className="stat-label">Down</div><div className="stat-value text-down">{summary.down}</div></div>
          <div className="stat-card"><div className="stat-label">Avg uptime (24h)</div><div className="stat-value">{formatPct(summary.averageUptime24h)}</div></div>
          <div className="stat-card"><div className="stat-label">Open incidents</div><div className="stat-value">{summary.openIncidents}</div></div>
        </div>
      )}

      {loading ? (
        <p className="muted">Loading...</p>
      ) : monitors.length === 0 ? (
        <div className="card empty">
          <p>No monitors yet. Click <strong>+ Add monitor</strong> to start monitoring a URL.</p>
        </div>
      ) : (
        <div className="card table-wrap">
          <table>
            <thead>
              <tr>
                <th>Status</th>
                <th>Name</th>
                <th>Uptime (24h)</th>
                <th>Response</th>
                <th>Interval</th>
                <th>Last check</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {monitors.map((m) => (
                <tr key={m.id}>
                  <td><StatusBadge status={m.status} /></td>
                  <td>
                    <Link to={`/monitors/${m.id}`} className="monitor-name">{m.name}</Link>
                    <div className="muted small">{m.url}</div>
                  </td>
                  <td>{formatPct(m.uptime24h)}</td>
                  <td>{formatMs(m.lastResponseTimeMs)}</td>
                  <td>{m.intervalSeconds}s</td>
                  <td>{timeAgo(m.lastCheckedAt)}</td>
                  <td className="row-actions">
                    <button className="btn btn-small" onClick={() => togglePause(m)}>
                      {m.active ? 'Pause' : 'Resume'}
                    </button>
                    <button className="btn btn-small btn-danger" onClick={() => remove(m)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showForm && (
        <MonitorForm
          onClose={() => setShowForm(false)}
          onSaved={() => {
            setShowForm(false);
            load();
          }}
        />
      )}
    </>
  );
}
