"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.disable2FA = exports.verify2FA = exports.generate2FA = exports.login = void 0;
const bcryptjs_1 = __importDefault(require("bcryptjs"));
const prisma_1 = __importDefault(require("../db/prisma"));
const jsonwebtoken_1 = __importDefault(require("jsonwebtoken"));
const otplib_1 = require("otplib");
const qrcode_1 = __importDefault(require("qrcode"));
const login = async (req, res) => {
    try {
        const { username, password } = req.body;
        if (!username || !password) {
            res.status(400).json({ error: 'Username and password are required' });
            return;
        }
        const JWT_SECRET = process.env.JWT_SECRET;
        if (!JWT_SECRET) {
            console.error('FATAL ERROR: JWT_SECRET environment variable is missing.');
            res.status(500).json({ error: 'Internal Server Configuration Error' });
            return;
        }
        const user = await prisma_1.default.user.findUnique({ where: { username } });
        if (!user) {
            res.status(401).json({ error: 'Invalid credentials' });
            return;
        }
        const isMatch = await bcryptjs_1.default.compare(password, user.password_hash);
        if (!isMatch) {
            res.status(401).json({ error: 'Invalid credentials' });
            return;
        }
        if (user.isTwoFactorEnabled) {
            const { totp } = req.body;
            if (!totp) {
                res.status(401).json({ error: '2FA token required', requires2FA: true });
                return;
            }
            if (!user.twoFactorSecret) {
                res.status(500).json({ error: '2FA is enabled but secret is missing' });
                return;
            }
            const isValid = otplib_1.authenticator.check(totp, user.twoFactorSecret);
            if (!isValid) {
                res.status(401).json({ error: 'Invalid 2FA token' });
                return;
            }
        }
        const token = jsonwebtoken_1.default.sign({ id: user.id, username: user.username, role: user.role }, JWT_SECRET, { expiresIn: '24h' });
        res.status(200).json({
            token: token,
            user: { id: user.id, username: user.username, role: user.role }
        });
    }
    catch (error) {
        res.status(500).json({ error: 'Internal server error' });
    }
};
exports.login = login;
const generate2FA = async (req, res) => {
    try {
        const userId = req.user?.id;
        if (!userId) {
            res.status(401).json({ error: 'Unauthorized' });
            return;
        }
        const user = await prisma_1.default.user.findUnique({ where: { id: userId } });
        if (!user) {
            res.status(404).json({ error: 'User not found' });
            return;
        }
        const secret = otplib_1.authenticator.generateSecret();
        const otpauthUrl = otplib_1.authenticator.keyuri(user.username, 'GPS Cam Portal', secret);
        await prisma_1.default.user.update({
            where: { id: userId },
            data: { twoFactorSecret: secret, isTwoFactorEnabled: false }
        });
        const qrCodeImage = await qrcode_1.default.toDataURL(otpauthUrl);
        res.status(200).json({ secret, qrCodeImage });
    }
    catch (error) {
        console.error('Generate 2FA error:', error);
        res.status(500).json({ error: 'Failed to generate 2FA secret' });
    }
};
exports.generate2FA = generate2FA;
const verify2FA = async (req, res) => {
    try {
        const userId = req.user?.id;
        const { token } = req.body;
        if (!userId || !token) {
            res.status(400).json({ error: 'Token is required' });
            return;
        }
        const user = await prisma_1.default.user.findUnique({ where: { id: userId } });
        if (!user || !user.twoFactorSecret) {
            res.status(400).json({ error: '2FA secret not found. Generate it first.' });
            return;
        }
        const isValid = otplib_1.authenticator.check(token, user.twoFactorSecret);
        if (isValid) {
            await prisma_1.default.user.update({
                where: { id: userId },
                data: { isTwoFactorEnabled: true }
            });
            res.status(200).json({ message: '2FA enabled successfully' });
        }
        else {
            res.status(400).json({ error: 'Invalid TOTP token' });
        }
    }
    catch (error) {
        console.error('Verify 2FA error:', error);
        res.status(500).json({ error: 'Failed to verify 2FA' });
    }
};
exports.verify2FA = verify2FA;
const disable2FA = async (req, res) => {
    try {
        const userId = req.user?.id;
        if (!userId) {
            res.status(401).json({ error: 'Unauthorized' });
            return;
        }
        await prisma_1.default.user.update({
            where: { id: userId },
            data: { isTwoFactorEnabled: false, twoFactorSecret: null }
        });
        res.status(200).json({ message: '2FA disabled successfully' });
    }
    catch (error) {
        console.error('Disable 2FA error:', error);
        res.status(500).json({ error: 'Failed to disable 2FA' });
    }
};
exports.disable2FA = disable2FA;
//# sourceMappingURL=authController.js.map