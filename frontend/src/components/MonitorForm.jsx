import { useState } from 'react';
import api, { errorMessage } from '../api';

const empty = {
  name: '',
  url: 'https://',
  httpMethod: 'GET',
  intervalSeconds: 60,
  timeoutSeconds: 10,
  expectedStatus: 200,
  webhookUrl: '',
  alertEmail: '',
};

export default function MonitorForm({ monitor, onClose, onSaved }) {
  const [form, setForm] = useState(monitor ? { ...empty, ...monitor, webhookUrl: monitor.webhookUrl || '', alertEmail: monitor.alertEmail || '' } : empty);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setSaving(true);
    const payload = {
      name: form.name,
      url: form.url,
      httpMethod: form.httpMethod,
      intervalSeconds: Number(form.intervalSeconds),
      timeoutSeconds: Number(form.timeoutSeconds),
      expectedStatus: Number(form.expectedStatus),
      webhookUrl: form.webhookUrl,
      alertEmail: form.alertEmail,
    };
    try {
      const res = monitor
        ? await api.put(`/monitors/${monitor.id}`, payload)
        : await api.post('/monitors', payload);
      onSaved(res.data);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <form className="modal" onClick={(e) => e.stopPropagation()} onSubmit={submit}>
        <h2>{monitor ? 'Edit monitor' : 'Add monitor'}</h2>
        {error && <div className="alert alert-error">{error}</div>}

        <label>Name
          <input value={form.name} onChange={set('name')} placeholder="My Website" required />
        </label>
        <label>URL
          <input value={form.url} onChange={set('url')} placeholder="https://example.com/health" required />
        </label>

        <div className="grid-3">
          <label>Method
            <select value={form.httpMethod} onChange={set('httpMethod')}>
              <option>GET</option>
              <option>HEAD</option>
            </select>
          </label>
          <label>Interval (sec)
            <input type="number" min="10" max="3600" value={form.intervalSeconds} onChange={set('intervalSeconds')} required />
          </label>
          <label>Timeout (sec)
            <input type="number" min="1" max="60" value={form.timeoutSeconds} onChange={set('timeoutSeconds')} required />
          </label>
        </div>

        <label>Expected status code
          <input type="number" min="100" max="599" value={form.expectedStatus} onChange={set('expectedStatus')} required />
        </label>
        <label>Alert email <span className="muted">(optional, default: your account email)</span>
          <input type="email" value={form.alertEmail} onChange={set('alertEmail')} placeholder="you@example.com" />
        </label>
        <label>Webhook URL <span className="muted">(optional, Slack / Discord)</span>
          <input value={form.webhookUrl} onChange={set('webhookUrl')} placeholder="https://hooks.slack.com/services/..." />
        </label>

        <div className="modal-actions">
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? 'Saving...' : monitor ? 'Save changes' : 'Create monitor'}
          </button>
        </div>
      </form>
    </div>
  );
}
