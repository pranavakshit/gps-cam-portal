"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.deleteUser = exports.changePassword = exports.updateUser = exports.createUser = exports.getUsers = void 0;
const bcryptjs_1 = __importDefault(require("bcryptjs"));
const prisma_1 = __importDefault(require("../db/prisma"));
const getUsers = async (req, res) => {
    try {
        const users = await prisma_1.default.user.findMany({
            select: {
                id: true,
                username: true,
                role: true,
            },
        });
        res.status(200).json(users);
    }
    catch (error) {
        res.status(500).json({ error: 'Internal server error' });
    }
};
exports.getUsers = getUsers;
const createUser = async (req, res) => {
    try {
        const { username, password, role } = req.body;
        if (!username || !password || !role) {
            res.status(400).json({ error: 'Username, password, and role are required' });
            return;
        }
        const existingUser = await prisma_1.default.user.findUnique({ where: { username } });
        if (existingUser) {
            res.status(400).json({ error: 'Username already exists' });
            return;
        }
        const passwordHash = await bcryptjs_1.default.hash(password, 10);
        const newUser = await prisma_1.default.user.create({
            data: {
                username,
                password_hash: passwordHash,
                role,
            },
            select: {
                id: true,
                username: true,
                role: true,
            },
        });
        res.status(201).json(newUser);
    }
    catch (error) {
        res.status(500).json({ error: 'Internal server error' });
    }
};
exports.createUser = createUser;
const updateUser = async (req, res) => {
    try {
        const { id } = req.params;
        const { username, role } = req.body;
        const user = await prisma_1.default.user.update({
            where: { id: parseInt(id) },
            data: { username, role },
            select: {
                id: true,
                username: true,
                role: true,
            },
        });
        res.status(200).json(user);
    }
    catch (error) {
        res.status(500).json({ error: 'Failed to update user' });
    }
};
exports.updateUser = updateUser;
const changePassword = async (req, res) => {
    try {
        const { id } = req.params;
        const { password } = req.body;
        if (!password) {
            res.status(400).json({ error: 'New password is required' });
            return;
        }
        const passwordHash = await bcryptjs_1.default.hash(password, 10);
        await prisma_1.default.user.update({
            where: { id: parseInt(id) },
            data: { password_hash: passwordHash },
        });
        res.status(200).json({ message: 'Password updated successfully' });
    }
    catch (error) {
        res.status(500).json({ error: 'Failed to change password' });
    }
};
exports.changePassword = changePassword;
const deleteUser = async (req, res) => {
    try {
        const { id } = req.params;
        // Optional: prevent deleting the last admin
        if (parseInt(id) === 1) { // Assuming ID 1 is the default admin
            res.status(403).json({ error: 'Cannot delete the default administrator account' });
            return;
        }
        await prisma_1.default.user.delete({
            where: { id: parseInt(id) },
        });
        res.status(200).json({ message: 'User deleted successfully' });
    }
    catch (error) {
        res.status(500).json({ error: 'Failed to delete user' });
    }
};
exports.deleteUser = deleteUser;
//# sourceMappingURL=userController.js.map