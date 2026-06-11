import { Request, Response } from 'express';
import { PrismaClient } from '@prisma/client';
// import bcrypt from 'bcrypt';
import prisma from '../db/prisma';

import jwt from 'jsonwebtoken';

const JWT_SECRET: string = process.env.JWT_SECRET || 'fallback-secret-for-dev';

export const login = async (req: Request, res: Response) => {
  try {
    const { username, password } = req.body;

    // TODO: Hash password check when user creation is added
    // For MVP, we might seed a dummy user or just accept anything for now if not set
    // const user = await prisma.user.findUnique({ where: { username } });

    // Generate actual JWT so it passes the authMiddleware verify
    const token = jwt.sign({ username, role: 'admin' }, JWT_SECRET, { expiresIn: '1h' });

    res.status(200).json({
      token: token,
      user: { username, role: 'admin' }
    });
  } catch (error) {
    res.status(500).json({ error: 'Internal server error' });
  }
};
