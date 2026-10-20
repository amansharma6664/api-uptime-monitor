import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import api, { errorMessage } from '../api';
import IncidentTable from '../components/IncidentTable.jsx';
import MonitorForm from '../components/MonitorForm.jsx';
import StatusBadge from '../components/StatusBadge.jsx';
import { formatMs, formatPct, formatTime, timeAgo } from '../utils';

const RANGES = [
  { label: '1h', hours: 1 },
  { label: '24h', hours: 24 },
  { label: '7d', hours: 168 },
  { label: '30d', hours: 720 },
];

function Dot({ cx, cy, payload }) {
  if (cx === undefined || cy === undefined) return null;
  if (!payload.down) return <circle cx={cx} cy={cy} r={0} />;
  return <circle cx={cx} cy={cy} r={5} fill="#dc2626" stroke="#fff" strokeWidth={1.5} />;
}

export default function MonitorDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [monitor, setMonitor] = useState(null);
  const [stats, setStats] = useState(null);
  const [results, setResults] = useState([]);
  const [incidents, setIncidents] = useState([]);
  const [hours, setHours] = useState(24);
  const [error, setError] = useState('');
  const [checking, setChecking] = useState(false);
  const [showEdit, setShowEdit] = useState(false);

  const load = useCallback(async () => {
    try {
      const [m, s, r, i] = await Promise.all([
        api.get(`/monitors/${id}`),
        api.get(`/monitors/${id}/stats`),
        api.get(`/monitors/${id}/results?hours=${hours}`),
        api.get(`/monitors/${id}/incidents`),
      ]);
      setMonitor(m.data);
      setStats(s.data);
      setResults(r.data);
      setIncidents(i.data);
      setError('');
    } catch (err) {
      if (err.response?.status === 404) {
        navigate('/');
        return;
      }
      setError(errorMessage(err));
    }
  }, [id, hours, navigate]);

  useEffect(() => {
    load();
    const timer = setInterval(load, 10000);
    return () => clearInterval(timer);
  }, [load]);

  const checkNow = async () => {
    setChecking(true);
    try {
      await api.post(`/monitors/${id}/check`);
      await load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setChecking(false);
    }
  };

  const togglePause = async () => {
    try {
      await api.post(`/monitors/${id}/${monitor.active ? 'pause' : 'resume'}`);
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  const remove = async () => {
    if (!window.confirm(`Delete monitor "${monitor.name}"? All its history will be removed.`)) return;
    try {
      await api.delete(`/monitors/${id}`);
      navigate('/');
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  if (!monitor) {
    return error ? <div className="alert alert-error">{error}</div> : <p className="muted">Loading...</p>;
  }

  const chartData = results.map((r) => ({
    id: r.id,
    time: new Date(r.checkedAt).getTime(),
    ms: r.responseTimeMs,
    down: r.status === 'DOWN',
  }));
  const recent = [...results].reverse().slice(0, 15);
  const tickFormat = (t) =>
    hours <= 24
      ? new Date(t).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      : new Date(t).toLocaleDateString([], { month: 'short', day: 'numeric' });

  return (
    <>
      <p className="muted"><Link to="/">← All monitors</Link></p>

      <div className="page-header">
        <div>
          <h1>{monitor.name} <StatusBadge status={monitor.status} /></h1>
          <p className="muted">
            {monitor.httpMethod} {monitor.url} · every {monitor.intervalSeconds}s · expects HTTP {monitor.expectedStatus} · last check {timeAgo(monitor.lastCheckedAt)}
          </p>
        </div>
        <div className="header-actions">
          <button className="btn" onClick={checkNow} disabled={checking || !monitor.active}>
            {checking ? 'Checking...' : 'Check now'}
          </button>
          <button className="btn" onClick={togglePause}>{monitor.active ? 'Pause' : 'Resume'}</button>
          <button className="btn" onClick={() => setShowEdit(true)}>Edit</button>
          <button className="btn btn-danger" onClick={remove}>Delete</button>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {stats && (
        <div className="stat-grid">
          <div className="stat-card"><div className="stat-label">Uptime 24h</div><div className="stat-value">{formatPct(stats.uptime24h)}</div></div>
          <div className="stat-card"><div className="stat-label">Uptime 7d</div><div className="stat-value">{formatPct(stats.uptime7d)}</div></div>
          <div className="stat-card"><div className="stat-label">Uptime 30d</div><div className="stat-value">{formatPct(stats.uptime30d)}</div></div>
          <div className="stat-card"><div className="stat-label">Avg response (24h)</div><div className="stat-value">{formatMs(stats.avgResponseMs24h)}</div></div>
          <div className="stat-card"><div className="stat-label">Failed checks (24h)</div><div className="stat-value">{stats.failedChecks24h} <span className="muted small">/ {stats.totalChecks24h}</span></div></div>
        </div>
      )}

      <div className="card">
        <div className="card-header">
          <h3>Response time</h3>
          <div className="range-tabs">
            {RANGES.map((r) => (
              <button key={r.hours} className={`tab ${hours === r.hours ? 'tab-active' : ''}`} onClick={() => setHours(r.hours)}>
                {r.label}
              </button>
            ))}
          </div>
        </div>
        {chartData.length === 0 ? (
          <p className="muted empty">No data yet. The first check runs within a few seconds.</p>
        ) : (
          <div className="chart-box">
            <ResponsiveContainer width="100%" height={280}>
              <LineChart data={chartData} margin={{ top: 10, right: 20, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis dataKey="time" type="number" scale="time" domain={['dataMin', 'dataMax']} tickFormatter={tickFormat} fontSize={12} />
                <YAxis unit=" ms" fontSize={12} width={70} />
                <Tooltip
                  labelFormatter={(t) => new Date(t).toLocaleString()}
                  formatter={(value, name, item) => [`${value} ms${item.payload.down ? ' (DOWN)' : ''}`, 'Response time']}
                />
                <Line type="monotone" dataKey="ms" stroke="#2563eb" strokeWidth={2} dot={<Dot />} activeDot={{ r: 5 }} isAnimationActive={false} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        )}
      </div>

      <div className="two-col">
        <div className="card">
          <h3>Recent checks</h3>
          {recent.length === 0 ? (
            <p className="muted empty">No checks yet.</p>
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr><th>Result</th><th>HTTP</th><th>Time</th><th>When</th></tr>
                </thead>
                <tbody>
                  {recent.map((r) => (
                    <tr key={r.id}>
                      <td><span className={`badge ${r.status === 'UP' ? 'badge-up' : 'badge-down'}`}>{r.status}</span></td>
                      <td>{r.statusCode ?? '—'}</td>
                      <td>{formatMs(r.responseTimeMs)}</td>
                      <td title={r.errorMessage || ''}>{formatTime(r.checkedAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <div className="card">
          <h3>Incidents</h3>
          <IncidentTable incidents={incidents} showMonitor={false} />
        </div>
      </div>

      {showEdit && (
        <MonitorForm
          monitor={monitor}
          onClose={() => setShowEdit(false)}
          onSaved={() => {
            setShowEdit(false);
            load();
          }}
        />
      )}
    </>
  );
}
