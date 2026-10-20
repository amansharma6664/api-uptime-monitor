const LABELS = { UP: 'Up', DOWN: 'Down', PENDING: 'Pending', PAUSED: 'Paused' };

export default function StatusBadge({ status }) {
  return <span className={`badge badge-${(status || 'PENDING').toLowerCase()}`}>{LABELS[status] || status}</span>;
}
