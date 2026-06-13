import { Router } from 'express';
import { getUsers, createUser, updateUser, changePassword, deleteUser } from '../controllers/userController';
import { authenticateJWT, requireAdmin } from '../middleware/authMiddleware';

const router = Router();

// All user routes require admin access
router.use(authenticateJWT, requireAdmin);

router.get('/', getUsers);
router.post('/', createUser);
router.put('/:id', updateUser);
router.put('/:id/password', changePassword);
router.delete('/:id', deleteUser);

export default router;
