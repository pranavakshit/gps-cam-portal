import { Router } from 'express';
import { listContainers, getContainerLogs, restartContainer, startContainer, getDockerStats, pruneDocker } from '../controllers/dockerController';
import { authenticateJWT, requireAdmin, requireAdminOrVisitor } from '../middleware/authMiddleware';

const router = Router();

// All docker routes require authentication
router.use(authenticateJWT);

router.get('/containers', requireAdminOrVisitor, listContainers);
router.get('/containers/:id/logs', requireAdminOrVisitor, getContainerLogs);
router.post('/containers/:id/restart', requireAdmin, restartContainer);
router.post('/containers/:id/start', requireAdmin, startContainer);
router.get('/stats', requireAdminOrVisitor, getDockerStats);
router.post('/prune', requireAdmin, pruneDocker);

export default router;
