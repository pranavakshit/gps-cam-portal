import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';

export interface AuthRequest extends Request {
  user?: {
    id: number;
    username: string;
    role: string;
  };
}

export const authenticateJWT = (req: AuthRequest, res: Response, next: NextFunction): void => {
  const authHeader = req.headers.authorization;

  if (authHeader) {
    const token = authHeader.split(' ')[1];

    if (!token) {
      res.status(401).json({ error: 'Unauthorized: No token provided' });
      return;
    }

    const JWT_SECRET: string = process.env.JWT_SECRET || 'fallback-secret-for-dev';

    jwt.verify(token, JWT_SECRET, (err: jwt.VerifyErrors | null, user: any) => {
      if (err) {
        console.error("JWT Verify Error:", err);
        res.status(403).json({ error: 'Forbidden or Token Expired', details: err.message });
        return;
      }
      req.user = user as any;
      next();
    });
  } else {
    res.status(401).json({ error: 'Unauthorized' });
  }
};

export const requireAdmin = (req: AuthRequest, res: Response, next: NextFunction): void => {
  if (req.user && req.user.role === 'ADMIN') {
    next();
  } else {
    res.status(403).json({ error: 'Forbidden: Admin access required' });
  }
};
