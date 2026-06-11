import { Request, Response } from 'express';
import { PrismaClient } from '@prisma/client';
// import bcrypt from 'bcrypt';
// import jwt from 'jsonwebtoken';

const prisma = new PrismaClient();

export const login = async (req: Request, res: Response) => {
  try {
    const { username, password } = req.body;

    // TODO: Hash password check when user creation is added
    // For MVP, we might seed a dummy user or just accept anything for now if not set
    // const user = await prisma.user.findUnique({ where: { username } });

    res.status(200).json({
      token: 'dummy-jwt-token',
      user: { username, role: 'admin' }
    });
  } catch (error) {
    res.status(500).json({ error: 'Internal server error' });
  }
};
