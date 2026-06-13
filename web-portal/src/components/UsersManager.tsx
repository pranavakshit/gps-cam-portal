import React, { useState, useEffect } from 'react';
import { User, Shield, Edit2, Trash2, Plus, Key, RefreshCw } from 'lucide-react';
import '../pages/Dashboard.css';

interface UserModel {
  id: number;
  username: string;
  role: string;
}

const UsersManager: React.FC = () => {
  const [users, setUsers] = useState<UserModel[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState<'create' | 'edit'>('create');
  
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [formData, setFormData] = useState({ username: '', password: '', role: 'user' });
  const [passwordData, setPasswordData] = useState({ password: '' });

  const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:5000';

  const fetchUsers = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`${API_URL}/api/users`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (!response.ok) throw new Error('Failed to fetch users');
      const data = await response.json();
      setUsers(data);
    } catch (err: any) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const openCreateModal = () => {
    setModalMode('create');
    setFormData({ username: '', password: '', role: 'user' });
    setIsModalOpen(true);
  };

  const openEditModal = (user: UserModel) => {
    setModalMode('edit');
    setSelectedUserId(user.id);
    setFormData({ username: user.username, password: '', role: user.role });
    setIsModalOpen(true);
  };

  const openPasswordModal = (user: UserModel) => {
    setSelectedUserId(user.id);
    setPasswordData({ password: '' });
    setIsPasswordModalOpen(true);
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Are you sure you want to delete this user?')) return;
    try {
      const token = localStorage.getItem('token');
      const res = await fetch(`${API_URL}/api/users/${id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (!res.ok) {
        const data = await res.json();
        throw new Error(data.error || 'Failed to delete');
      }
      fetchUsers();
    } catch (err: any) {
      alert(err.message);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const token = localStorage.getItem('token');
      const url = modalMode === 'create' ? `${API_URL}/api/users` : `${API_URL}/api/users/${selectedUserId}`;
      const method = modalMode === 'create' ? 'POST' : 'PUT';

      const body = modalMode === 'create' 
        ? { username: formData.username, password: formData.password, role: formData.role }
        : { username: formData.username, role: formData.role };

      const res = await fetch(url, {
        method,
        headers: { 
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(body)
      });

      if (!res.ok) {
        const data = await res.json();
        throw new Error(data.error || 'Operation failed');
      }
      
      setIsModalOpen(false);
      fetchUsers();
    } catch (err: any) {
      alert(err.message);
    }
  };

  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const token = localStorage.getItem('token');
      const res = await fetch(`${API_URL}/api/users/${selectedUserId}/password`, {
        method: 'PUT',
        headers: { 
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ password: passwordData.password })
      });

      if (!res.ok) throw new Error('Password change failed');
      setIsPasswordModalOpen(false);
      alert('Password updated successfully');
    } catch (err: any) {
      alert(err.message);
    }
  };

  if (loading) return <div className="admin-placeholder"><h2 style={{ color: 'var(--text-color)' }}>Loading Users...</h2></div>;

  return (
    <div className="users-manager animate-fade-in" style={{ padding: '24px' }}>
      <div className="content-header" style={{ marginBottom: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2 style={{ color: 'var(--text-color)', margin: 0 }}>User Management</h2>
        <div style={{ display: 'flex', gap: '12px' }}>
          <button className="btn btn-outline" onClick={fetchUsers} title="Refresh Users List">
            <RefreshCw size={18} style={{ marginRight: '8px' }} /> Sync
          </button>
          <button className="btn btn-primary" onClick={openCreateModal}>
            <Plus size={18} style={{ marginRight: '8px' }} /> Add User
          </button>
        </div>
      </div>

      {error && <div className="error-message" style={{ marginBottom: '16px' }}>{error}</div>}

      <div className="glass-panel" style={{ padding: '24px', overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', color: 'var(--text-color)' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid var(--border-color)', textAlign: 'left' }}>
              <th style={{ padding: '12px' }}>ID</th>
              <th style={{ padding: '12px' }}>Username</th>
              <th style={{ padding: '12px' }}>Role</th>
              <th style={{ padding: '12px', textAlign: 'right' }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map(user => (
              <tr key={user.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
                <td style={{ padding: '12px' }}>{user.id}</td>
                <td style={{ padding: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <User size={16} /> {user.username}
                </td>
                <td style={{ padding: '12px' }}>
                  <span style={{ 
                    padding: '4px 8px', 
                    borderRadius: '4px', 
                    fontSize: '0.85em',
                    backgroundColor: user.role === 'ADMIN' ? 'var(--primary-color)' : 'var(--bg-color)',
                    color: user.role === 'ADMIN' ? 'var(--bg-color)' : 'var(--text-color)',
                    border: '1px solid var(--border-color)'
                  }}>
                    {user.role === 'ADMIN' ? <><Shield size={12} style={{ display: 'inline', marginRight: '4px' }}/>{user.role}</> : user.role}
                  </span>
                </td>
                <td style={{ padding: '12px', textAlign: 'right' }}>
                  <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
                    <button className="btn icon-btn" onClick={() => openEditModal(user)} title="Edit Role/Username"><Edit2 size={16}/></button>
                    <button className="btn icon-btn" onClick={() => openPasswordModal(user)} title="Change Password"><Key size={16}/></button>
                    <button className="btn icon-btn" onClick={() => handleDelete(user.id)} title="Delete User" style={{ color: '#ff4444' }}><Trash2 size={16}/></button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {isModalOpen && (
        <div className="modal-overlay" style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100 }}>
          <div className="glass-panel" style={{ padding: '24px', width: '400px', maxWidth: '90%' }}>
            <h3 style={{ color: 'var(--text-color)', marginBottom: '16px' }}>{modalMode === 'create' ? 'Create User' : 'Edit User'}</h3>
            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <input 
                type="text" 
                className="input-field" 
                placeholder="Username" 
                value={formData.username}
                onChange={e => setFormData({...formData, username: e.target.value})}
                required
              />
              {modalMode === 'create' && (
                <input 
                  type="password" 
                  className="input-field" 
                  placeholder="Password" 
                  value={formData.password}
                  onChange={e => setFormData({...formData, password: e.target.value})}
                  required
                />
              )}
              <select 
                className="input-field" 
                value={formData.role}
                onChange={e => setFormData({...formData, role: e.target.value})}
                style={{ backgroundColor: 'var(--bg-color)', color: 'var(--text-color)' }}
              >
                <option value="user">User</option>
                <option value="ADMIN">Administrator</option>
              </select>
              <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end', marginTop: '8px' }}>
                <button type="button" className="btn" onClick={() => setIsModalOpen(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Save</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {isPasswordModalOpen && (
        <div className="modal-overlay" style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100 }}>
          <div className="glass-panel" style={{ padding: '24px', width: '400px', maxWidth: '90%' }}>
            <h3 style={{ color: 'var(--text-color)', marginBottom: '16px' }}>Change Password</h3>
            <form onSubmit={handlePasswordSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <input 
                type="password" 
                className="input-field" 
                placeholder="New Password" 
                value={passwordData.password}
                onChange={e => setPasswordData({ password: e.target.value })}
                required
              />
              <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end', marginTop: '8px' }}>
                <button type="button" className="btn" onClick={() => setIsPasswordModalOpen(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary">Update</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default UsersManager;
