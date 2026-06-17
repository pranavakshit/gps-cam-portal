import { Request, Response } from 'express';
import { AuthRequest } from '../middleware/authMiddleware';
export declare const login: (req: Request, res: Response) => Promise<void>;
export declare const generate2FA: (req: AuthRequest, res: Response) => Promise<void>;
export declare const verify2FA: (req: AuthRequest, res: Response) => Promise<void>;
export declare const disable2FA: (req: AuthRequest, res: Response) => Promise<void>;
//# sourceMappingURL=authController.d.ts.map