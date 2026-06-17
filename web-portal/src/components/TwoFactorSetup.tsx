import React, { useState } from 'react';
import { ShieldAlert, Key, CheckCircle, XCircle } from 'lucide-react';

interface TwoFactorSetupProps {
  onClose: () => void;
}

const TwoFactorSetup: React.FC<TwoFactorSetupProps> = ({ onClose }) => {
  const [setupStep, setSetupStep] = useState<'initial' | 'setup' | 'success'>('initial');
  const [qrCodeUrl, setQrCodeUrl] = useState('');
  const [secret, setSecret] = useState('');
  const [totp, setTotp] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:5000';

  const handleGenerate = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await fetch(`${API_URL}/api/auth/2fa/generate`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      if (!response.ok) throw new Error('Failed to generate 2FA secret');
      
      const data = await response.json();
      setQrCodeUrl(data.qrCodeImage);
      setSecret(data.secret);
      setSetupStep('setup');
    } catch (err: any) {
      setError(err.message || 'An error occurred');
    } finally {
      setLoading(false);
    }
  };

  const handleVerify = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await fetch(`${API_URL}/api/auth/2fa/verify`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify({ token: totp })
      });
      
      if (!response.ok) {
        const data = await response.json();
        throw new Error(data.error || 'Invalid 2FA token');
      }
      
      localStorage.setItem('is2FAEnabled', 'true');
      setSetupStep('success');
    } catch (err: any) {
      setError(err.message || 'An error occurred');
    } finally {
      setLoading(false);
    }
  };

  const handleRequestDisable = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await fetch(`${API_URL}/api/auth/2fa/request-disable`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      if (!response.ok) throw new Error('Failed to request 2FA disable');
      alert('Your request to disable 2FA has been sent to the admin.');
      onClose();
    } catch (err: any) {
      setError(err.message || 'An error occurred');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay animate-fade-in" style={{
      position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
      backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex',
      alignItems: 'center', justifyContent: 'center', zIndex: 1000
    }}>
      <div className="modal-content glass-panel" style={{ padding: '32px', maxWidth: '400px', width: '100%', borderRadius: '16px', position: 'relative' }}>
        <button onClick={onClose} style={{ position: 'absolute', top: '16px', right: '16px', background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-color)' }}>
          <XCircle size={24} />
        </button>

        <h2 style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '24px', color: 'var(--text-color)' }}>
          <ShieldAlert size={28} /> Security Settings
        </h2>

        {error && <div className="error-message" style={{ marginBottom: '16px' }}>{error}</div>}

        {setupStep === 'initial' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <p style={{ color: 'var(--text-color)' }}>
              Two-Factor Authentication (2FA) adds an extra layer of security to your account by requiring a code from your authenticator app when you sign in.
            </p>
            <button className="btn btn-primary" onClick={handleGenerate} disabled={loading}>
              {loading ? 'Processing...' : 'Setup 2FA'}
            </button>
            <button className="btn btn-secondary" onClick={handleRequestDisable} disabled={loading}>
              Request Admin to Disable 2FA
            </button>
          </div>
        )}

        {setupStep === 'setup' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', alignItems: 'center' }}>
            <p style={{ color: 'var(--text-color)', textAlign: 'center' }}>
              Scan this QR code with Google Authenticator or a similar app.
            </p>
            {qrCodeUrl && <img src={qrCodeUrl} alt="2FA QR Code" style={{ width: '200px', height: '200px', backgroundColor: '#fff', padding: '8px', borderRadius: '8px' }} />}
            
            <div style={{ width: '100%', padding: '12px', backgroundColor: 'rgba(255, 0, 0, 0.1)', borderRadius: '8px', border: '1px solid rgba(255,0,0,0.3)', marginTop: '8px' }}>
              <p style={{ color: '#ff4d4f', fontSize: '12px', margin: '0 0 8px 0', fontWeight: 'bold' }}>
                WARNING: NEVER share this code with anyone!
              </p>
              <p style={{ color: 'var(--text-color)', fontSize: '14px', margin: 0 }}>
                If you are on your phone and cannot scan the QR code, manually enter this secret into your authenticator app. <strong>The 6-digit code expires every 30 seconds.</strong>
              </p>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '8px', backgroundColor: 'rgba(0,0,0,0.2)', padding: '8px', borderRadius: '4px' }}>
                <Key size={16} />
                <code style={{ flex: 1, letterSpacing: '1px' }}>{secret}</code>
              </div>
            </div>

            <input
              type="text"
              placeholder="Enter 6-digit code"
              value={totp}
              onChange={(e) => setTotp(e.target.value.replace(/\D/g, '').slice(0, 6))}
              className="input-field"
              style={{ width: '100%', textAlign: 'center', letterSpacing: '4px', fontSize: '18px' }}
            />
            
            <button className="btn btn-primary" onClick={handleVerify} disabled={loading || totp.length !== 6} style={{ width: '100%' }}>
              {loading ? 'Verifying...' : 'Verify & Enable'}
            </button>
          </div>
        )}

        {setupStep === 'success' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', alignItems: 'center' }}>
            <CheckCircle size={48} color="#4caf50" />
            <h3 style={{ color: 'var(--text-color)', margin: 0 }}>2FA Enabled Successfully!</h3>
            <p style={{ color: 'var(--text-color)', textAlign: 'center' }}>
              Your account is now protected with Two-Factor Authentication.
            </p>
            <button className="btn btn-primary" onClick={onClose} style={{ width: '100%' }}>
              Close
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

export default TwoFactorSetup;
