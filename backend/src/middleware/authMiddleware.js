"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.requireAdmin = exports.authenticateJWT = void 0;
const jsonwebtoken_1 = __importDefault(require("jsonwebtoken"));
const authenticateJWT = (req, res, next) => {
    const authHeader = req.headers.authorization;
    if (authHeader) {
        const token = authHeader.split(' ')[1];
        if (!token) {
            res.status(401).json({ error: 'Unauthorized: No token provided' });
            return;
        }
        const JWT_SECRET = process.env.JWT_SECRET;
        if (!JWT_SECRET) {
            console.error('FATAL ERROR: JWT_SECRET environment variable is missing.');
            res.status(500).json({ error: 'Internal Server Configuration Error' });
            return;
        }
        jsonwebtoken_1.default.verify(token, JWT_SECRET, (err, user) => {
            if (err) {
                console.error("JWT Verify Error:", err);
                res.status(403).json({ error: 'Forbidden or Token Expired', details: err.message });
                return;
            }
            req.user = user;
            next();
        });
    }
    else {
        res.status(401).json({ error: 'Unauthorized' });
    }
};
exports.authenticateJWT = authenticateJWT;
const requireAdmin = (req, res, next) => {
    if (req.user && req.user.role === 'ADMIN') {
        next();
    }
    else {
        res.status(403).json({ error: 'Forbidden: Admin access required' });
    }
};
exports.requireAdmin = requireAdmin;
//# sourceMappingURL=authMiddleware.js.map