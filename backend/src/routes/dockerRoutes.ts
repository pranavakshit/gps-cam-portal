import { Router } from 'express';
import { listContainers, getContainerLogs, restartContainer, startContainer, getDockerStats, pruneDocker } from '../controllers/dockerController';
import { authenticateJWT, requireAdmin } from '../middleware/authMiddleware';

const router = Router();

// All docker routes require admin access
router.use(authenticateJWT, requireAdmin);

router.get('/containers', listContainers);
router.get('/containers/:id/logs', getContainerLogs);
router.post('/containers/:id/restart', restartContainer);
router.post('/containers/:id/start', startContainer);
router.get('/stats', getDockerStats);
router.post('/prune', pruneDocker);

export default router;
