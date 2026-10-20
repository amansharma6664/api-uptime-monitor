import { useEffect, useState } from 'react';
import api, { errorMessage } from '../api';
import IncidentTable from '../components/IncidentTable.jsx';

export default function Incidents() {
  const [incidents, setIncidents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        const res = await api.get('/incidents?limit=50');
        setIncidents(res.data);
        setError('');
      } catch (err) {
        setError(errorMessage(err));
      } finally {
        setLoading(false);
      }
    };
    load();
    const timer = setInterval(load, 10000);
    return () => clearInterval(timer);
  }, []);

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Incidents</h1>
          <p className="muted">A monitor is marked down after 2 failed checks in a row</p>
        </div>
      </div>
      {error && <div className="alert alert-error">{error}</div>}
      {loading ? <p className="muted">Loading...</p> : <div className="card"><IncidentTable incidents={incidents} /></div>}
    </>
  );
}
