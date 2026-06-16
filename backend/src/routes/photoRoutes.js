"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const photoController_1 = require("../controllers/photoController");
const authMiddleware_1 = require("../middleware/authMiddleware");
const router = (0, express_1.Router)();
// GET /api/photos
// Protected endpoint for admin and users to view gallery
router.get('/', authMiddleware_1.authenticateJWT, photoController_1.getPhotos);
// POST /api/photos/upload
// Endpoint for Android app to upload photos. For MVP, we might keep it open or require an app-level token.
// The app can upload multiple photos at once.
router.post('/upload', photoController_1.upload.array('photos', 5), photoController_1.uploadPhoto);
// POST /api/photos/:id/request-delete
router.post('/:id/request-delete', authMiddleware_1.authenticateJWT, photoController_1.requestDeletePhoto);
// POST /api/photos/:id/approve-delete
router.post('/:id/approve-delete', authMiddleware_1.authenticateJWT, photoController_1.approveDeletePhoto);
// POST /api/photos/:id/reject-delete
const photoController_2 = require("../controllers/photoController");
router.post('/:id/reject-delete', authMiddleware_1.authenticateJWT, photoController_2.rejectDeletePhoto);
// POST /api/photos/:id/complete-delete
router.post('/:id/complete-delete', authMiddleware_1.authenticateJWT, photoController_2.completeDeletePhoto);
// POST /api/photos/:id/abort-delete
router.post('/:id/abort-delete', authMiddleware_1.authenticateJWT, photoController_2.abortDeletePhoto);
// DELETE /api/photos/:id
// Protected endpoint to delete a photo (soft delete)
router.delete('/:id', authMiddleware_1.authenticateJWT, photoController_1.deletePhoto);
// POST /api/photos/:id/restore
// Protected endpoint to restore a soft-deleted photo
const photoController_3 = require("../controllers/photoController");
router.post('/:id/restore', authMiddleware_1.authenticateJWT, photoController_3.restorePhoto);
// DELETE /api/photos/:id/hard
// Protected endpoint to permanently delete a photo
router.delete('/:id/hard', authMiddleware_1.authenticateJWT, photoController_3.hardDeletePhoto);
exports.default = router;
//# sourceMappingURL=photoRoutes.js.map