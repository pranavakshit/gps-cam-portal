import React, { useState, useEffect } from 'react';
import { Camera, MapPin, Calendar, Download, Search, LogOut } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
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

  useEffect(() => {
    // Simulate API fetch
    setTimeout(() => {
      setPhotos(MOCK_PHOTOS);
    }, 500);
  }, []);

  const handleLogout = () => {
    navigate('/');
  };

  const handleDownload = (url: string, _filename: string) => {
    // In a real app, this would fetch the blob and trigger download
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
        <div className="nav-actions">
          <button className="btn btn-logout" onClick={handleLogout}>
            <LogOut size={18} /> Logout
          </button>
        </div>
      </nav>

      <main className="dashboard-content">
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
      </main>
    </div>
  );
};

export default Dashboard;
