import { Link } from 'react-router-dom';
import { formatDuration, formatTime } from '../utils';

export default function IncidentTable({ incidents, showMonitor = true }) {
  if (!incidents.length) {
    return <p className="muted empty">No incidents. Everything looks good.</p>;
  }
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {showMonitor && <th>Monitor</th>}
            <th>Status</th>
            <th>Started</th>
            <th>Duration</th>
            <th>Reason</th>
          </tr>
        </thead>
        <tbody>
          {incidents.map((i) => (
            <tr key={i.id}>
              {showMonitor && (
                <td><Link to={`/monitors/${i.monitorId}`}>{i.monitorName}</Link></td>
              )}
              <td>
                <span className={`badge ${i.ongoing ? 'badge-down' : 'badge-up'}`}>
                  {i.ongoing ? 'Ongoing' : 'Resolved'}
                </span>
              </td>
              <td>{formatTime(i.startedAt)}</td>
              <td>{formatDuration(i.durationSeconds)}</td>
              <td className="reason">{i.reason || '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
