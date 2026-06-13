import React, { useState, useEffect } from 'react';
import { Server, RefreshCw, Play, FileText } from 'lucide-react';
import '../pages/Dashboard.css';

interface Container {
  ID: string;
  Image: string;
  Command: string;
  CreatedAt: string;
  RunningFor: string;
  Ports: string;
  Status: string;
  Size: string;
  Names: string;
}

const DockerManager: React.FC = () => {
  const [containers, setContainers] = useState<Container[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  
  const [logs, setLogs] = useState('');
  const [isLogsModalOpen, setIsLogsModalOpen] = useState(false);
  const [activeContainerId, setActiveContainerId] = useState('');

  const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:5000';

  const fetchContainers = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`${API_URL}/api/docker/containers`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (!response.ok) throw new Error('Failed to fetch containers');
      const data = await response.json();
      setContainers(data);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchContainers();
  }, []);

  const handleAction = async (action: 'restart' | 'start', id: string) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`${API_URL}/api/docker/containers/${id}/${action}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (!response.ok) throw new Error(`Failed to ${action} container`);
      alert(`Container ${action}ed successfully`);
      fetchContainers();
    } catch (err: any) {
      alert(err.message);
    }
  };

  const fetchLogs = async (id: string) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`${API_URL}/api/docker/containers/${id}/logs`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (!response.ok) throw new Error('Failed to fetch logs');
      const data = await response.json();
      setLogs(data.logs);
      setActiveContainerId(id);
      setIsLogsModalOpen(true);
    } catch (err: any) {
      alert(err.message);
    }
  };

  if (loading) return <div className="admin-placeholder"><h2 style={{ color: 'var(--text-color)' }}>Loading Docker Info...</h2></div>;

  return (
    <div className="docker-manager animate-fade-in" style={{ padding: '24px' }}>
      <div className="content-header" style={{ marginBottom: '24px' }}>
        <h2 style={{ color: 'var(--text-color)' }}><Server style={{ display: 'inline', marginRight: '8px', verticalAlign: 'text-bottom' }} /> System Management</h2>
        <button className="btn icon-btn" onClick={fetchContainers} title="Refresh">
          <RefreshCw size={18} />
        </button>
      </div>

      {error && <div className="error-message" style={{ marginBottom: '16px' }}>{error}</div>}

      <div className="glass-panel" style={{ padding: '24px', overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', color: 'var(--text-color)' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid var(--border-color)', textAlign: 'left' }}>
              <th style={{ padding: '12px' }}>Name</th>
              <th style={{ padding: '12px' }}>Image</th>
              <th style={{ padding: '12px' }}>Status</th>
              <th style={{ padding: '12px' }}>Ports</th>
              <th style={{ padding: '12px', textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {containers.map(c => (
              <tr key={c.ID} style={{ borderBottom: '1px solid var(--border-color)' }}>
                <td style={{ padding: '12px', fontWeight: 'bold' }}>{c.Names}</td>
                <td style={{ padding: '12px', fontSize: '0.9em', opacity: 0.8 }}>{c.Image}</td>
                <td style={{ padding: '12px' }}>
                  <span style={{ 
                    padding: '4px 8px', 
                    borderRadius: '4px', 
                    fontSize: '0.85em',
                    backgroundColor: c.Status.includes('Up') ? '#2e7d32' : '#c62828',
                    color: 'white'
                  }}>
                    {c.Status}
                  </span>
                </td>
                <td style={{ padding: '12px', fontSize: '0.85em' }}>{c.Ports}</td>
                <td style={{ padding: '12px', textAlign: 'right' }}>
                  <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                    <button className="btn icon-btn" onClick={() => fetchLogs(c.ID)} title="View Logs"><FileText size={16}/></button>
                    {!c.Status.includes('Up') && (
                      <button className="btn icon-btn" onClick={() => handleAction('start', c.ID)} title="Start"><Play size={16}/></button>
                    )}
                    <button className="btn icon-btn" onClick={() => handleAction('restart', c.ID)} title="Restart"><RefreshCw size={16}/></button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {isLogsModalOpen && (
        <div className="modal-overlay" style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100 }}>
          <div className="glass-panel" style={{ padding: '24px', width: '800px', maxWidth: '95%', maxHeight: '90vh', display: 'flex', flexDirection: 'column' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '16px' }}>
              <h3 style={{ color: 'var(--text-color)', margin: 0 }}>Container Logs</h3>
              <button className="btn icon-btn" onClick={() => fetchLogs(activeContainerId)}><RefreshCw size={16}/></button>
            </div>
            <pre style={{ 
              backgroundColor: '#000', 
              color: '#00ff00', 
              padding: '16px', 
              borderRadius: '8px',
              overflow: 'auto',
              flex: 1,
              fontFamily: 'monospace',
              fontSize: '12px'
            }}>
              {logs || 'No logs available.'}
            </pre>
            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '16px' }}>
              <button className="btn" onClick={() => setIsLogsModalOpen(false)}>Close</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default DockerManager;
