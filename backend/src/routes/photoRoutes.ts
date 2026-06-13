import { Router } from 'express';
import { upload, uploadPhoto, getPhotos, deletePhoto, requestDeletePhoto, approveDeletePhoto } from '../controllers/photoController';
import { authenticateJWT } from '../middleware/authMiddleware';

const router = Router();

// GET /api/photos
// Protected endpoint for admin and users to view gallery
router.get('/', authenticateJWT, getPhotos);

// POST /api/photos/upload
// Endpoint for Android app to upload photos. For MVP, we might keep it open or require an app-level token.
// The app can upload multiple photos at once.
router.post('/upload', upload.array('photos', 5), uploadPhoto);

// POST /api/photos/:id/request-delete
router.post('/:id/request-delete', authenticateJWT, requestDeletePhoto);

// POST /api/photos/:id/approve-delete
router.post('/:id/approve-delete', authenticateJWT, approveDeletePhoto);

// POST /api/photos/:id/reject-delete
import { rejectDeletePhoto, completeDeletePhoto, abortDeletePhoto } from '../controllers/photoController';
router.post('/:id/reject-delete', authenticateJWT, rejectDeletePhoto);

// POST /api/photos/:id/complete-delete
router.post('/:id/complete-delete', authenticateJWT, completeDeletePhoto);

// POST /api/photos/:id/abort-delete
router.post('/:id/abort-delete', authenticateJWT, abortDeletePhoto);

// DELETE /api/photos/:id
// Protected endpoint to delete a photo (soft delete)
router.delete('/:id', authenticateJWT, deletePhoto);

// POST /api/photos/:id/restore
// Protected endpoint to restore a soft-deleted photo
import { restorePhoto, hardDeletePhoto } from '../controllers/photoController';
router.post('/:id/restore', authenticateJWT, restorePhoto);

// DELETE /api/photos/:id/hard
// Protected endpoint to permanently delete a photo
router.delete('/:id/hard', authenticateJWT, hardDeletePhoto);

export default router;
