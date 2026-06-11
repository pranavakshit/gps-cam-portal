import { Router } from 'express';
import { upload, uploadPhoto, getPhotos } from '../controllers/photoController';
import { authenticateJWT } from '../middleware/authMiddleware';

const router = Router();

// GET /api/photos
// Protected endpoint for admin to view gallery
router.get('/', authenticateJWT, getPhotos);

// POST /api/photos/upload
// Endpoint for Android app to upload photos. For MVP, we might keep it open or require an app-level token.
// The app can upload multiple photos at once.
router.post('/upload', upload.array('photos', 5), uploadPhoto);

export default router;
