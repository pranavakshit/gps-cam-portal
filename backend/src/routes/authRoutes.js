"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const authController_1 = require("../controllers/authController");
const authMiddleware_1 = require("../middleware/authMiddleware");
const router = (0, express_1.Router)();
// POST /api/auth/login
router.post('/login', authController_1.login);
// 2FA Routes (Protected)
router.post('/2fa/generate', authMiddleware_1.authenticateJWT, authController_1.generate2FA);
router.post('/2fa/verify', authMiddleware_1.authenticateJWT, authController_1.verify2FA);
router.post('/2fa/disable', authMiddleware_1.authenticateJWT, authController_1.disable2FA);
exports.default = router;
//# sourceMappingURL=authRoutes.js.map