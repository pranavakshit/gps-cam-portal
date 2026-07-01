import { Router } from 'express';
import multer from 'multer';
import { 
  getStates,
  getDistricts,
  getSubDistricts,
  getVillages,
  getUlbs,
  getWards,
  importLgdData,
  searchLocations,
  getOfflineBundle,
  createLocation,
  updateLocation,
  deleteLocation
} from '../controllers/locationController';
import { authenticateJWT, forbidVisitor } from '../middleware/authMiddleware';

const router = Router();
import fs from 'fs';
import path from 'path';

// Ensure temp directory exists
const tempDir = path.join(__dirname, '../../uploads/temp');
if (!fs.existsSync(tempDir)) {
  fs.mkdirSync(tempDir, { recursive: true });
}

const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, tempDir);
  },
  filename: function (req, file, cb) {
    cb(null, 'lgd-upload-' + Date.now() + '.zip');
  }
});
const upload = multer({ storage: storage });

// Public endpoints for Android App
router.get('/states', getStates);
router.get('/states/:id/districts', getDistricts);
router.get('/districts/:id/subdistricts', getSubDistricts);
router.get('/subdistricts/:id/villages', getVillages);
router.get('/districts/:id/ulbs', getUlbs);
router.get('/ulbs/:id/wards', getWards);
router.get('/search', searchLocations);
router.get('/states/:id/offline-bundle', getOfflineBundle);

// Protected Admin Endpoints
router.post('/import', authenticateJWT, forbidVisitor, upload.single('zipfile'), importLgdData);

// CRUD Endpoints for Drill-Down Editor
router.get('/search', authenticateJWT, searchLocations);
router.post('/', authenticateJWT, forbidVisitor, createLocation);
router.put('/:id', authenticateJWT, forbidVisitor, updateLocation);
router.delete('/:id', authenticateJWT, forbidVisitor, deleteLocation);

export default router;
