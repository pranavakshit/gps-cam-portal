import { Router } from 'express';
import { getUsers, createUser, updateUser, changePassword, deleteUser, disableUser2FA } from '../controllers/userController';
import { authenticateJWT, requireAdmin, requireAdminOrVisitor } from '../middleware/authMiddleware';

const router = Router();

// All user routes require authentication
router.use(authenticateJWT);

router.get('/', requireAdminOrVisitor, getUsers);
router.post('/', requireAdmin, createUser);
router.put('/:id', requireAdmin, updateUser);
router.put('/:id/password', requireAdmin, changePassword);
router.put('/:id/disable-2fa', requireAdmin, disableUser2FA);
router.delete('/:id', requireAdmin, deleteUser);

export default router;
