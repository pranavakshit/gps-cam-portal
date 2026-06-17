import { Request, Response } from 'express';
import { PrismaClient } from '@prisma/client';
import bcrypt from 'bcryptjs';
import prisma from '../db/prisma';

import jwt from 'jsonwebtoken';
import { authenticator } from 'otplib';
import QRCode from 'qrcode';
import { AuthRequest } from '../middleware/authMiddleware';
export const login = async (req: Request, res: Response): Promise<void> => {
  try {
    const { username, password } = req.body;

    if (!username || !password) {
      res.status(400).json({ error: 'Username and password are required' });
      return;
    }

    const JWT_SECRET: string | undefined = process.env.JWT_SECRET;
    if (!JWT_SECRET) {
      console.error('FATAL ERROR: JWT_SECRET environment variable is missing.');
      res.status(500).json({ error: 'Internal Server Configuration Error' });
      return;
    }

    const user = await prisma.user.findUnique({ where: { username } });

    if (!user) {
      res.status(401).json({ error: 'Invalid credentials' });
      return;
    }

    const isMatch = await bcrypt.compare(password, user.password_hash);
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

      const isValid = authenticator.check(totp, user.twoFactorSecret);
      if (!isValid) {
        res.status(401).json({ error: 'Invalid 2FA token' });
        return;
      }
    }

    const token = jwt.sign(
      { id: user.id, username: user.username, role: user.role }, 
      JWT_SECRET, 
      { expiresIn: '24h' }
    );

    res.status(200).json({ 
      token, 
      user: { 
        id: user.id, 
        username: user.username, 
        role: user.role,
        isTwoFactorEnabled: user.isTwoFactorEnabled 
      } 
    });
  } catch (error) {
    res.status(500).json({ error: 'Internal server error' });
  }
};

export const generate2FA = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const userId = req.user?.id;
    if (!userId) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    const user = await prisma.user.findUnique({ where: { id: userId } });
    if (!user) {
      res.status(404).json({ error: 'User not found' });
      return;
    }

    const secret = authenticator.generateSecret();
    const otpauthUrl = authenticator.keyuri(user.username, 'GPS Cam Portal', secret);

    await prisma.user.update({
      where: { id: userId },
      data: { twoFactorSecret: secret, isTwoFactorEnabled: false }
    });

    const qrCodeImage = await QRCode.toDataURL(otpauthUrl);
    res.status(200).json({ secret, qrCodeImage });
  } catch (error) {
    console.error('Generate 2FA error:', error);
    res.status(500).json({ error: 'Failed to generate 2FA secret' });
  }
};

export const verify2FA = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const userId = req.user?.id;
    const { token } = req.body;

    if (!userId || !token) {
      res.status(400).json({ error: 'Token is required' });
      return;
    }

    const user = await prisma.user.findUnique({ where: { id: userId } });
    if (!user || !user.twoFactorSecret) {
      res.status(400).json({ error: '2FA secret not found. Generate it first.' });
      return;
    }

    const isValid = authenticator.check(token, user.twoFactorSecret);

    if (isValid) {
      await prisma.user.update({
        where: { id: userId },
        data: { isTwoFactorEnabled: true }
      });
      res.status(200).json({ message: '2FA enabled successfully' });
    } else {
      res.status(400).json({ error: 'Invalid TOTP token' });
    }
  } catch (error) {
    console.error('Verify 2FA error:', error);
    res.status(500).json({ error: 'Failed to verify 2FA' });
  }
};

export const requestDisable2FA = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const userId = req.user?.id;
    if (!userId) {
      res.status(401).json({ error: 'Unauthorized' });
      return;
    }

    await prisma.user.update({
      where: { id: userId },
      data: { twoFactorDisableRequested: true }
    });

    res.status(200).json({ message: '2FA disable request sent to admin' });
  } catch (error) {
    console.error('Request disable 2FA error:', error);
    res.status(500).json({ error: 'Failed to request 2FA disable' });
  }
};
