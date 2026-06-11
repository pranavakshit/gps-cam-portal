import { Router } from 'express';
import { getLocations, createLocation } from '../controllers/locationController';
import { authenticateJWT } from '../middleware/authMiddleware';

const router = Router();

// GET /api/locations
// Open endpoint for the Android app to fetch available locations
router.get('/', getLocations);

// POST /api/locations
// Protected endpoint for admin to add new locations
router.post('/', authenticateJWT, createLocation);

export default router;
