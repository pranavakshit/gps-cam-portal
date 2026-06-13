import React, { useState, useEffect } from 'react';
import { Upload, Map, CheckCircle, AlertTriangle, Search, ChevronRight, Edit2, Save, X, Trash2, Plus } from 'lucide-react';
import './LocationsManager.css';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:5000';

interface LocationItem {
  id: number;
  lgdCode: number;
  name: string;
  type?: string;
  path?: string;
}

const LocationsManager: React.FC = () => {
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState<number>(0);
  const [message, setMessage] = useState<{ text: string, type: 'success' | 'error' } | null>(null);
  
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<LocationItem[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  
  // Drill-down State
  const [hierarchyPath, setHierarchyPath] = useState<{id: number, name: string, type: string}[]>([]);
  const [items, setItems] = useState<LocationItem[]>([]);
  const [loadingItems, setLoadingItems] = useState(false);
  
  // Editing State
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editName, setEditName] = useState('');
  
  // Adding state
  const [isAdding, setIsAdding] = useState(false);
  const [newName, setNewName] = useState('');
  const [newCode, setNewCode] = useState('');

  const fetchItems = async () => {
    setLoadingItems(true);
    try {
      let url = `${API_URL}/api/locations/states`;
      const len = hierarchyPath.length;
      
      if (len === 1) { // State selected, fetch Districts
        url = `${API_URL}/api/locations/states/${hierarchyPath[0].id}/districts`;
      } else if (len === 2) { // District selected, fetch SubDistricts & ULBs
        const [subDistrictsRes, ulbsRes] = await Promise.all([
           fetch(`${API_URL}/api/locations/districts/${hierarchyPath[1].id}/subdistricts`),
           fetch(`${API_URL}/api/locations/districts/${hierarchyPath[1].id}/ulbs`)
        ]);
        const subDistricts = subDistrictsRes.ok ? await subDistrictsRes.json() : [];
        const ulbs = ulbsRes.ok ? await ulbsRes.json() : [];
        
        // Add type tags for the UI so they know what they are clicking
        const mappedSub = subDistricts.map((s: any) => ({ ...s, type: 'SubDistrict' }));
        const mappedUlb = ulbs.map((u: any) => ({ ...u, type: 'ULB' }));
        
        setItems([...mappedSub, ...mappedUlb]);
        return;
      } else if (len === 3) { // SubDistrict or ULB selected
        const parent = hierarchyPath[2];
        if (parent.type === 'SubDistrict') {
          url = `${API_URL}/api/locations/subdistricts/${parent.id}/villages`;
        } else {
          url = `${API_URL}/api/locations/ulbs/${parent.id}/wards`;
        }
      }
      
      const res = await fetch(url);
      if (res.ok) {
        setItems(await res.json());
      }
    } catch (err) {
      console.error('Failed to fetch items', err);
    } finally {
      setLoadingItems(false);
    }
  };

  useEffect(() => {
    if (!isSearching) {
      fetchItems();
    }
  }, [hierarchyPath, isSearching]);

  // Search Logic
  useEffect(() => {
    const delayDebounceFn = setTimeout(async () => {
      if (searchQuery.length >= 2) {
        setIsSearching(true);
        try {
          const res = await fetch(`${API_URL}/api/locations/search?q=${encodeURIComponent(searchQuery)}`, {
            headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
          });
          if (res.ok) {
            setSearchResults(await res.json());
          }
        } catch (e) {
          console.error(e);
        }
      } else {
        setIsSearching(false);
      }
    }, 500);

    return () => clearTimeout(delayDebounceFn);
  }, [searchQuery]);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setFile(e.target.files[0]);
      setMessage(null);
    }
  };

  const [abortController, setAbortController] = useState<AbortController | null>(null);

  const handleUpload = async () => {
    if (!file) return;
    setUploading(true);
    setUploadProgress(0);
    setMessage(null);
    const formData = new FormData();
    formData.append('zipfile', file);
    
    const controller = new AbortController();
    setAbortController(controller);
    
    try {
      const res = await fetch(`${API_URL}/api/locations/import`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` },
        body: formData,
        signal: controller.signal
      });
      
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        setMessage({ text: err.error || 'Failed to sync data', type: 'error' });
        setUploading(false);
        setAbortController(null);
        return;
      }
      
      const reader = res.body?.getReader();
      if (!reader) throw new Error("No readable stream");
      const decoder = new TextDecoder();
      
      let doneReading = false;
      while (!doneReading) {
        const { done, value } = await reader.read();
        if (done) break;
        const chunk = decoder.decode(value, { stream: true });
        const lines = chunk.split('\n\n');
        for (const line of lines) {
            if (line.trim().startsWith('data:')) {
                try {
                    const data = JSON.parse(line.replace('data:', '').trim());
                    if (data.error) {
                        setMessage({ text: data.error, type: 'error' });
                        doneReading = true;
                        break;
                    }
                    if (data.progress !== undefined) {
                        setUploadProgress(data.progress);
                        setMessage({ text: data.message || 'Syncing...', type: 'success' });
                    }
                    if (data.done) {
                        setMessage({ text: 'LGD Data synchronized successfully!', type: 'success' });
                        setFile(null);
                        fetchItems();
                        doneReading = true;
                        break;
                    }
                } catch (e) {
                    console.error('Error parsing stream data', e);
                }
            }
        }
      }
    } catch (err: any) {
      if (err.name === 'AbortError') {
        setMessage({ text: 'Sync cancelled by user.', type: 'error' });
      } else {
        setMessage({ text: 'Error connecting to server.', type: 'error' });
      }
    } finally {
      setUploading(false);
      setAbortController(null);
      setTimeout(() => setUploadProgress(0), 3000);
    }
  };

  const handleCancel = () => {
    if (abortController) {
      abortController.abort();
    }
  };

  const currentLevelType = () => {
    const len = hierarchyPath.length;
    if (len === 0) return 'State';
    if (len === 1) return 'District';
    if (len === 2) return 'SubDistrict'; // Defaulting to SubDistrict for adding new items
    if (len === 3) return hierarchyPath[2].type === 'SubDistrict' ? 'Village' : 'Ward';
    return '';
  };

  const handleUpdate = async (lgdCode: number, newName: string) => {
    try {
      const res = await fetch(`${API_URL}/api/locations/${lgdCode}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify({ type: currentLevelType(), name: newName })
      });
      if (res.ok) {
        setEditingId(null);
        fetchItems();
      }
    } catch (e) {
      console.error(e);
    }
  };

  const handleDelete = async (lgdCode: number) => {
    if (!window.confirm("Are you sure you want to delete this location? Photos tagged with this location will remain but the location name will not be resolved.")) return;
    
    try {
      const res = await fetch(`${API_URL}/api/locations/${lgdCode}?type=${currentLevelType()}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
      });
      if (res.ok) {
        fetchItems();
      }
    } catch (e) {
      console.error(e);
    }
  };

  const handleAdd = async () => {
    if (!newName || !newCode) return;
    const parentCode = hierarchyPath.length > 0 ? hierarchyPath[hierarchyPath.length - 1].id : null;
    
    try {
      const res = await fetch(`${API_URL}/api/locations`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify({
          type: currentLevelType(),
          name: newName,
          lgdCode: parseInt(newCode),
          parentCode
        })
      });
      if (res.ok) {
        setIsAdding(false);
        setNewName('');
        setNewCode('');
        fetchItems();
      } else {
        alert("Failed to add. LGD Code might already exist.");
      }
    } catch (e) {
      console.error(e);
    }
  };

  const handleItemClick = (item: LocationItem) => {
    if (hierarchyPath.length >= 3) return; // Leaf nodes aren't clickable
    const newPath = [...hierarchyPath, { id: item.lgdCode, name: item.name, type: item.type || currentLevelType() }];
    setHierarchyPath(newPath);
    setSearchQuery('');
  };

  const handleSearchResultClick = (res: any) => {
    const parts = (res.path || '').split(' > ');
    const newPath: {id: number, name: string, type: string}[] = [];
    
    if (res.type === 'State') {
        // stay at root
    } else if (res.type === 'District') {
        newPath.push({ id: res.stateId, name: parts[0], type: 'State' });
    } else if (res.type === 'SubDistrict' || res.type === 'ULB') {
        newPath.push({ id: res.stateId, name: parts[0], type: 'State' });
        newPath.push({ id: res.districtId, name: parts[1], type: 'District' });
    } else if (res.type === 'Village') {
        newPath.push({ id: res.stateId, name: parts[0], type: 'State' });
        newPath.push({ id: res.districtId, name: parts[1], type: 'District' });
        newPath.push({ id: res.subDistrictId, name: parts[2], type: 'SubDistrict' });
    } else if (res.type === 'Ward') {
        newPath.push({ id: res.stateId, name: parts[0], type: 'State' });
        newPath.push({ id: res.districtId, name: parts[1], type: 'District' });
        newPath.push({ id: res.ulbId, name: parts[2], type: 'ULB' });
    }

    setHierarchyPath(newPath);
    setSearchQuery('');
    setIsSearching(false);
  };

  const navigateTo = (index: number) => {
    setHierarchyPath(hierarchyPath.slice(0, index + 1));
  };

  return (
    <div className="locations-manager animate-fade-in">
      <div className="manager-header">
        <h2><Map size={24} /> Locations Data Explorer</h2>
        <p>Upload official LGD Data or manually edit the database hierarchy.</p>
      </div>

      <div className="locations-layout">
        <div className="upload-sidebar glass-panel">
          <div className="upload-box compact">
            <Upload size={32} className="upload-icon" />
            <h3>Upload LGD ZIP</h3>
            <p className="small-text">Synchronize database from official LGD package.</p>
            <input type="file" accept=".zip,.xls,.xlsx" onChange={handleFileChange} className="file-input" id="zip-upload" />
            <label htmlFor="zip-upload" className="btn btn-secondary file-label btn-sm">Choose File</label>
            {file && (
              <div className="selected-file small-text" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', marginTop: '8px' }}>
                <span>{file.name}</span>
                <button className="icon-btn danger" onClick={() => setFile(null)} disabled={uploading} aria-label="Remove File">
                  <X size={14} />
                </button>
              </div>
            )}
            
            {(uploading || uploadProgress > 0) && (
              <div className="progress-bar-container" style={{ width: '100%', height: '8px', backgroundColor: '#e2e8f0', borderRadius: '4px', marginTop: '10px', overflow: 'hidden' }}>
                <div className="progress-bar-fill" style={{ width: `${uploadProgress}%`, height: '100%', backgroundColor: '#4f46e5', transition: 'width 0.3s ease' }}></div>
              </div>
            )}
            
            <div style={{ display: 'flex', gap: '8px', marginTop: '16px', justifyContent: 'center' }}>
              <button className="btn btn-primary upload-btn btn-sm" onClick={handleUpload} disabled={!file || uploading}>
                {uploading ? 'Processing...' : 'Sync Data'}
              </button>
              {uploading && (
                <button className="btn btn-secondary upload-btn btn-sm" onClick={handleCancel}>
                  Cancel Sync
                </button>
              )}
            </div>
          </div>
          {message && (
            <div className={`message-alert small-text ${message.type}`}>
              {message.type === 'success' ? <CheckCircle size={16} /> : <AlertTriangle size={16} />}
              <span>{message.text}</span>
            </div>
          )}
        </div>

        <div className="data-explorer glass-panel">
          <div className="explorer-toolbar">
            <div className="search-bar">
              <Search size={18} />
              <input 
                type="text" 
                placeholder="Search across all locations..." 
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
              />
            </div>
          </div>

          {!isSearching ? (
            <>
              <div className="breadcrumb">
                <span className="crumb" onClick={() => setHierarchyPath([])}>India</span>
                {hierarchyPath.map((step, idx) => (
                  <React.Fragment key={idx}>
                    <ChevronRight size={14} className="crumb-sep" />
                    <span className="crumb" onClick={() => navigateTo(idx)}>{step.name}</span>
                  </React.Fragment>
                ))}
              </div>

              <div className="list-container">
                {loadingItems ? (
                  <div className="loader">Loading...</div>
                ) : (
                  <>
                    <div className="list-actions">
                      <button className="btn btn-sm btn-outline" onClick={() => setIsAdding(true)}>
                        <Plus size={14}/> Add Custom {currentLevelType()}
                      </button>
                    </div>
                    
                    {isAdding && (
                      <div className="list-item adding-item">
                        <input type="number" placeholder="Code" value={newCode} onChange={e => setNewCode(e.target.value)} className="inline-input code-input"/>
                        <input type="text" placeholder="Name" value={newName} onChange={e => setNewName(e.target.value)} className="inline-input"/>
                        <div className="item-actions">
                          <button className="icon-btn success" onClick={handleAdd}><Save size={16}/></button>
                          <button className="icon-btn danger" onClick={() => setIsAdding(false)}><X size={16}/></button>
                        </div>
                      </div>
                    )}

                    {items.length === 0 && !isAdding && <div className="empty-state">No locations found.</div>}
                    
                    {items.map(item => (
                      <div className="list-item" key={item.lgdCode}>
                        <div className="item-content" onClick={() => handleItemClick(item)}>
                          <span className="item-code">{item.lgdCode}</span>
                          {editingId === item.lgdCode ? (
                            <input 
                              type="text" 
                              value={editName} 
                              onChange={(e) => setEditName(e.target.value)}
                              className="inline-input"
                              onClick={(e) => e.stopPropagation()}
                            />
                          ) : (
                            <span className="item-name">{item.name}</span>
                          )}
                        </div>
                        <div className="item-actions">
                          {editingId === item.lgdCode ? (
                            <>
                              <button className="icon-btn success" onClick={() => handleUpdate(item.lgdCode, editName)}><Save size={16}/></button>
                              <button className="icon-btn" onClick={() => setEditingId(null)}><X size={16}/></button>
                            </>
                          ) : (
                            <>
                              <button className="icon-btn" onClick={() => { setEditingId(item.lgdCode); setEditName(item.name); }}><Edit2 size={16}/></button>
                              <button className="icon-btn danger" onClick={() => handleDelete(item.lgdCode)}><Trash2 size={16}/></button>
                            </>
                          )}
                        </div>
                      </div>
                    ))}
                  </>
                )}
              </div>
            </>
          ) : (
            <div className="search-results list-container">
              <h3>Search Results</h3>
              {searchResults.length === 0 ? (
                <div className="empty-state">No matching locations found.</div>
              ) : (
                searchResults.map(res => (
                  <div className="list-item search-item" key={`${res.type}-${res.id}`} onClick={() => handleSearchResultClick(res)} style={{ cursor: 'pointer' }}>
                    <div className="item-content">
                      <span className="item-code badge">{res.type}</span>
                      <div className="search-item-details">
                        <span className="item-name">{res.name} (Code: {res.lgdCode})</span>
                        <span className="item-path">{res.path}</span>
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default LocationsManager;
