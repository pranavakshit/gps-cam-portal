"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const dockerController_1 = require("../controllers/dockerController");
const authMiddleware_1 = require("../middleware/authMiddleware");
const router = (0, express_1.Router)();
// All docker routes require admin access
router.use(authMiddleware_1.authenticateJWT, authMiddleware_1.requireAdmin);
router.get('/containers', dockerController_1.listContainers);
router.get('/containers/:id/logs', dockerController_1.getContainerLogs);
router.post('/containers/:id/restart', dockerController_1.restartContainer);
router.post('/containers/:id/start', dockerController_1.startContainer);
exports.default = router;
//# sourceMappingURL=dockerRoutes.js.map