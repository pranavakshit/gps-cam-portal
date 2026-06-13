import React, { useState, useEffect } from 'react';
import { Camera, MapPin, Calendar, Download, Search, LogOut, Trash2, AlertTriangle, Check, RefreshCw } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import LocationsManager from '../components/LocationsManager';
import UsersManager from '../components/UsersManager';
import DockerManager from '../components/DockerManager';
import './Dashboard.css';

interface Photo {
  id: number;
  locationName: string;
  latitude: number;
  longitude: number;
  timestamp: string;
  imageUrl: string;
  uploader: string;
  deletionStatus: string;
  deletionReason?: string;
}

const Dashboard: React.FC = () => {
  const navigate = useNavigate();
  const [photos, setPhotos] = useState<Photo[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [activeTab, setActiveTab] = useState<'gallery' | 'recycle-bin' | 'locations' | 'users' | 'docker'>('gallery');
  const userRole = localStorage.getItem('role') || 'user';
  const username = localStorage.getItem('username') || '';

  const fetchPhotos = async () => {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        navigate('/');
        return;
      }

      const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:5000';
      const isRecycleBin = activeTab === 'recycle-bin';
      const response = await fetch(`${API_URL}/api/photos${isRecycleBin ? '?recycle_bin=true' : ''}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        const data = await response.json();
        const formattedData = data.map((photo: any) => ({
          ...photo,
          latitude: Number(photo.latitude),
          longitude: Number(photo.longitude),
          imageUrl: `${API_URL}${photo.imageUrl}`
        }));
        setPhotos(formattedData);
      } else if (response.status === 401 || response.status === 403) {
        localStorage.removeItem('token');
        navigate('/');
      }
    } catch (error) {
      console.error('Error fetching photos:', error);
    }
  };

  useEffect(() => {
    if (activeTab === 'gallery' || activeTab === 'recycle-bin') {
      fetchPhotos();
    }
  }, [navigate, activeTab]);

  const [themePreference, setThemePreference] = useState(localStorage.getItem('theme-preference') || 'system');

  useEffect(() => {
    const applyTheme = () => {
      let activeTheme = themePreference;
      if (themePreference === 'system') {
        activeTheme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
      }
      document.documentElement.setAttribute('data-theme', activeTheme);
    };

    applyTheme();
    localStorage.setItem('theme-preference', themePreference);
  }, [themePreference]);

  const cycleTheme = () => {
    setThemePreference(prev => {
      if (prev === 'light') return 'dark';
      if (prev === 'dark') return 'system';
      return 'light';
    });
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    navigate('/');
  };

  const handleDownload = (url: string, _filename: string) => {
    window.open(url, '_blank');
  };

  // 1. Request Deletion (User or Admin requesting)
  const handleRequestDeletion = async (id: number) => {
    const reason = window.prompt("Please provide a reason for deleting this photo:");
    if (reason === null) return; // cancelled
    
    try {
      const token = localStorage.getItem('token');
      const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:5000';
      const response = await fetch(`${API_URL}/api/photos/${id}/request-delete`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ reason })
      });
      
      if (response.ok) {
        fetchPhotos();
      } else {
        const errorData = await response.json().catch(() => null);
        alert(`Failed: ${errorData?.error || response.statusText}`);
      }
    } catch (error) {
      console.error('Error:', error);
    }
  };

  // 2. Approve Deletion (Admin only)
  const handleApproveDeletion = async (id: number) => {
    try {
      const token = localStorage.getItem('token');
      const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:5000';
      const response = await fetch(`${API_URL}/api/photos/${id}/approve-delete`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      
      if (response.ok) {
        fetchPhotos();
      } else {
        const errorData = await response.json().catch(() => null);
        alert(`Failed: ${errorData?.error || response.statusText}`);
      }
    } catch (error) {
      console.error('Error:', error);
    }
  };

  // 3. Finalize Deletion (Actually Soft Deletes)
  const handleFinalizeDeletion = async (id: number) => {
    if (!window.confirm("Are you sure you want to finalize deletion? This will move the photo to the Recycle Bin.")) return;
    
    try {
      const token = localStorage.getItem('token');
      const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:5000';
      const response = await fetch(`${API_URL}/api/photos/${id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      
      if (response.ok) {
        setPhotos(photos.filter(p => p.id !== id));
      } else {
        const errorData = await response.json().catch(() => null);
        alert(`Failed: ${errorData?.error || response.statusText}`);
      }
    } catch (error) {
      console.error('Error:', error);
    }
  };

  const renderActionButtons = (photo: Photo) => {
    const isOwner = photo.uploader === username;
    const isAdmin = userRole === 'ADMIN';

    if (activeTab === 'recycle-bin') {
      return (
        <div className="status-badge" style={{backgroundColor: 'rgba(255, 50, 50, 0.8)', color: 'white', padding: '4px 8px', borderRadius: '4px', fontSize: '12px', fontWeight: 'bold'}}>
          Soft Deleted
        </div>
      );
    }

    if (photo.deletionStatus === 'USER_REQUESTED') {
      if (isAdmin) {
        return (
          <button className="btn btn-success icon-btn" onClick={() => handleApproveDeletion(photo.id)} title="Approve User's Deletion Request">
            <Check size={20} />
          </button>
        );
      } else {
        return <span className="status-badge pending">Pending Admin Approval</span>;
      }
    }

    if (photo.deletionStatus === 'ADMIN_APPROVED') {
      if (isOwner || isAdmin) {
        return (
          <button className="btn btn-danger icon-btn" onClick={() => handleFinalizeDeletion(photo.id)} title="Finalize Deletion">
            <Trash2 size={20} />
          </button>
        );
      }
    }

    if (photo.deletionStatus === 'ADMIN_REQUESTED') {
      if (isOwner) {
        return (
          <button className="btn btn-danger icon-btn" onClick={() => handleFinalizeDeletion(photo.id)} title="Approve Admin's Deletion Request">
            <Check size={20} />
          </button>
        );
      } else if (isAdmin) {
        return <span className="status-badge pending">Waiting for User Approval</span>;
      }
    }

    // Default 'NONE' state
    if (isOwner || isAdmin) {
      return (
        <button 
          className="btn btn-warning icon-btn"
          onClick={() => handleRequestDeletion(photo.id)}
          title={isAdmin ? "Request User to Delete" : "Request Admin to Delete"}
          style={{ marginLeft: '8px' }}
        >
          <AlertTriangle size={20} />
        </button>
      );
    }

    return null;
  };

  const filteredPhotos = photos.filter(photo => 
    photo.locationName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    photo.uploader.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="dashboard-layout animate-fade-in">
      <nav className="dashboard-nav glass-panel">
        <div className="nav-brand">
          <Camera size={28} className="brand-icon" />
          <h1>GPS Cam Portal</h1>
        </div>
        <div className="nav-tabs">
          <button 
            className={`tab-btn ${activeTab === 'gallery' ? 'active' : ''}`}
            onClick={() => setActiveTab('gallery')}
          >
            Photo Gallery
          </button>
          <button 
            className={`tab-btn ${activeTab === 'recycle-bin' ? 'active' : ''}`}
            onClick={() => setActiveTab('recycle-bin')}
          >
            Recycle Bin
          </button>
          <button 
            className={`tab-btn ${activeTab === 'locations' ? 'active' : ''}`}
            onClick={() => setActiveTab('locations')}
          >
            Locations
          </button>
          {userRole === 'ADMIN' && (
            <>
              <button 
                className={`tab-btn ${activeTab === 'users' ? 'active' : ''}`}
                onClick={() => setActiveTab('users')}
              >
                Users
              </button>
              <button 
                className={`tab-btn ${activeTab === 'docker' ? 'active' : ''}`}
                onClick={() => setActiveTab('docker')}
              >
                System Management
              </button>
            </>
          )}
        </div>
        <div className="nav-actions">
          <button className="btn icon-btn" onClick={cycleTheme} aria-label="Toggle theme">
             {/* ... theme svg omitted for brevity, keeping existing ... */}
             <RefreshCw size={18} />
          </button>
          <button className="btn btn-logout" onClick={handleLogout}>
            <LogOut size={18} /> Logout
          </button>
        </div>
      </nav>

      <main className="dashboard-content">
        {(activeTab === 'gallery' || activeTab === 'recycle-bin') ? (
          <>
            <div className="content-header">
              <h2>{activeTab === 'gallery' ? 'Photo Gallery' : 'Recycle Bin'}</h2>
              <div className="search-bar">
                <Search size={20} className="search-icon" />
                <input 
                  type="text" 
                  className="input-field with-icon" 
                  placeholder="Search by location or uploader..." 
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
            </div>

            <div className="gallery-grid">
              {filteredPhotos.map((photo) => (
                <div key={photo.id} className="photo-card glass-panel">
                  <div className="photo-wrapper">
                    <img src={photo.imageUrl} alt={photo.locationName} loading="lazy" />
                    <div className="photo-overlay">
                      <button 
                        className="btn btn-primary icon-btn"
                        onClick={() => handleDownload(photo.imageUrl, `photo-${photo.id}.jpg`)}
                        title="Download Photo"
                      >
                        <Download size={20} />
                      </button>
                      {renderActionButtons(photo)}
                    </div>
                  </div>
                  <div className="photo-info">
                    <h3>{photo.locationName}</h3>
                    <div className="info-row">
                      <MapPin size={16} />
                      <span>{photo.latitude.toFixed(4)}, {photo.longitude.toFixed(4)}</span>
                    </div>
                    <div className="info-row">
                      <Calendar size={16} />
                      <span>{new Date(photo.timestamp).toLocaleString()}</span>
                    </div>
                    <div className="info-row uploader">
                      <span>Uploaded by: {photo.uploader}</span>
                    </div>
                    {photo.deletionReason && (
                      <div className="info-row uploader" style={{color: 'red', marginTop: '4px'}}>
                        <span>Reason: {photo.deletionReason}</span>
                      </div>
                    )}
                  </div>
                </div>
              ))}
              {filteredPhotos.length === 0 && (
                <div className="no-results">
                  <p>No photos found matching your criteria.</p>
                </div>
              )}
            </div>
          </>
        ) : activeTab === 'locations' ? (
          <LocationsManager />
        ) : activeTab === 'users' && userRole === 'ADMIN' ? (
          <UsersManager />
        ) : activeTab === 'docker' && userRole === 'ADMIN' ? (
          <DockerManager />
        ) : null}
      </main>
    </div>
  );
};

export default Dashboard;
