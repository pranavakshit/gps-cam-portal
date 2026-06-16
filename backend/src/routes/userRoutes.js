"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const userController_1 = require("../controllers/userController");
const authMiddleware_1 = require("../middleware/authMiddleware");
const router = (0, express_1.Router)();
// All user routes require admin access
router.use(authMiddleware_1.authenticateJWT, authMiddleware_1.requireAdmin);
router.get('/', userController_1.getUsers);
router.post('/', userController_1.createUser);
router.put('/:id', userController_1.updateUser);
router.put('/:id/password', userController_1.changePassword);
router.delete('/:id', userController_1.deleteUser);
exports.default = router;
//# sourceMappingURL=userRoutes.js.map