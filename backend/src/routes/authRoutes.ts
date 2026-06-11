import { Router } from 'express';

const router = Router();

// POST /api/auth/login
router.post('/login', (req, res) => {
  // TODO: implement login logic
  res.status(200).json({ token: 'dummy-token', user: { id: 1, role: 'admin' } });
});

export default router;
