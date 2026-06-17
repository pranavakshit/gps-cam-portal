import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Camera, Lock, User } from 'lucide-react';
import './Login.css';

const Login: React.FC = () => {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [totp, setTotp] = useState('');
  const [requires2FA, setRequires2FA] = useState(false);
  const [error, setError] = useState('');

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:5000';
      const response = await fetch(`${API_URL}/api/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username, password, totp: requires2FA ? totp : undefined }),
      });

      if (response.ok) {
        const data = await response.json();
        localStorage.setItem('token', data.token);
        if (data.user && data.user.role) {
           localStorage.setItem('role', data.user.role);
           localStorage.setItem('is2FAEnabled', data.user.isTwoFactorEnabled ? 'true' : 'false');
        }
        navigate('/dashboard');
      } else if (response.status === 401) {
        const errorData = await response.json();
        if (errorData.requires2FA) {
          setRequires2FA(true);
          setError('');
        } else {
          setError(errorData.error || 'Invalid credentials');
        }
      } else {
        setError('Invalid credentials');
      }
    } catch (err) {
      setError('Failed to connect to server');
    }
  };

  return (
    <div className="login-container animate-fade-in">
      <div className="login-card glass-panel">
        <div className="login-header">
          <div className="logo-icon">
            <Camera size={32} />
          </div>
          <h2>Admin Portal</h2>
          <p>Login to view and manage GPS stamped photos</p>
        </div>

        {error && <div className="error-message">{error}</div>}

        <form onSubmit={handleLogin} className="login-form">
          {!requires2FA ? (
            <>
              <div className="input-group">
                <User className="input-icon" size={20} />
                <input
                  type="text"
                  className="input-field with-icon"
                  placeholder="Username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  required
                />
              </div>
              
              <div className="input-group">
                <Lock className="input-icon" size={20} />
                <input
                  type="password"
                  className="input-field with-icon"
                  placeholder="Password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </div>
            </>
          ) : (
            <div className="input-group">
              <Lock className="input-icon" size={20} />
              <input
                type="text"
                className="input-field with-icon"
                placeholder="6-digit Auth Code"
                value={totp}
                onChange={(e) => setTotp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                required
              />
            </div>
          )}

          <button type="submit" className="btn btn-primary login-btn">
            {requires2FA ? 'Verify 2FA' : 'Sign In'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default Login;
