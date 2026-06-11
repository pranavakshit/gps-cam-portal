import { Router } from 'express';

const router = Router();

// GET /api/locations
router.get('/', (req, res) => {
  // TODO: Fetch locations from DB
  res.status(200).json([]);
});

export default router;
