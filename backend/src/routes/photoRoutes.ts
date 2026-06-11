import { Router } from 'express';

const router = Router();

// GET /api/photos
router.get('/', (req, res) => {
  // TODO: Fetch photos from DB
  res.status(200).json([]);
});

// GET /api/photos/:id
router.get('/:id', (req, res) => {
  // TODO: Fetch photo details from DB
  res.status(200).json({});
});

// POST /api/photos/upload
router.post('/upload', (req, res) => {
  // TODO: Handle file upload and metadata insertion
  res.status(201).json({ message: 'Upload successful' });
});

export default router;
