import React, { useState, useEffect } from 'react';
import { Camera, MapPin, Calendar, Download, Search, LogOut } from 'lucide-react';
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
}

const MOCK_PHOTOS: Photo[] = [
  {
    id: 1,
    locationName: 'Central Park',
    latitude: 40.7812,
    longitude: -73.9665,
    timestamp: '2026-06-11T10:00:00Z',
    imageUrl: 'https://images.unsplash.com/photo-1513415564515-763d91423bdd?auto=format&fit=crop&w=600&q=80',
    uploader: 'John Doe',
  },
  {
    id: 2,
    locationName: 'Statue of Liberty',
    latitude: 40.6892,
    longitude: -74.0445,
    timestamp: '2026-06-10T14:30:00Z',
    imageUrl: 'https://images.unsplash.com/photo-1543789523-289cf1862cd2?auto=format&fit=crop&w=600&q=80',
    uploader: 'Jane Smith',
  },
  {
    id: 3,
    locationName: 'Times Square',
    latitude: 40.7580,
    longitude: -73.9855,
    timestamp: '2026-06-09T20:15:00Z',
    imageUrl: 'https://images.unsplash.com/photo-1534430480872-3498386e7856?auto=format&fit=crop&w=600&q=80',
    uploader: 'John Doe',
  }
];

const Dashboard: React.FC = () => {
  const navigate = useNavigate();
  const [photos, setPhotos] = useState<Photo[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [activeTab, setActiveTab] = useState<'gallery' | 'locations' | 'users' | 'docker'>('gallery');
  const userRole = localStorage.getItem('role') || 'user';

  useEffect(() => {
    if (activeTab === 'gallery') {
      const fetchPhotos = async () => {
        try {
          const token = localStorage.getItem('token');
          if (!token) {
            navigate('/');
            return;
          }

          const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:5000';
          const response = await fetch(`${API_URL}/api/photos`, {
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

    if (themePreference === 'system') {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
      const listener = () => applyTheme();
      mediaQuery.addEventListener('change', listener);
      return () => mediaQuery.removeEventListener('change', listener);
    }
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
            {themePreference === 'light' ? (
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ width: '18px', height: '18px' }}>
                <circle cx="12" cy="12" r="5"></circle>
                <line x1="12" y1="1" x2="12" y2="3"></line>
                <line x1="12" y1="21" x2="12" y2="23"></line>
                <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line>
                <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line>
                <line x1="1" y1="12" x2="3" y2="12"></line>
                <line x1="21" y1="12" x2="23" y2="12"></line>
                <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line>
                <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line>
              </svg>
            ) : themePreference === 'dark' ? (
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ width: '18px', height: '18px' }}>
                <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>
              </svg>
            ) : (
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ width: '18px', height: '18px' }}>
                <rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect>
                <line x1="8" y1="21" x2="16" y2="21"></line>
                <line x1="12" y1="17" x2="12" y2="21"></line>
              </svg>
            )}
          </button>
          <button className="btn btn-logout" onClick={handleLogout}>
            <LogOut size={18} /> Logout
          </button>
        </div>
      </nav>

      <main className="dashboard-content">
        {activeTab === 'gallery' ? (
          <>
            <div className="content-header">
              <h2>Photo Gallery</h2>
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
