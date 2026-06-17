import { Router } from 'express';
import { login, generate2FA, verify2FA, requestDisable2FA } from '../controllers/authController';
import { authenticateJWT } from '../middleware/authMiddleware';

const router = Router();

// POST /api/auth/login
router.post('/login', login);

// 2FA Routes (Protected)
router.post('/2fa/generate', authenticateJWT, generate2FA);
router.post('/2fa/verify', authenticateJWT, verify2FA);
router.post('/2fa/request-disable', authenticateJWT, requestDisable2FA);

export default router;
