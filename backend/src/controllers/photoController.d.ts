import { Request, Response } from 'express';
import { AuthRequest } from '../middleware/authMiddleware';
import multer from 'multer';
export declare const upload: multer.Multer;
export declare const uploadPhoto: (req: Request, res: Response) => Promise<void>;
export declare const getPhotos: (req: AuthRequest, res: Response) => Promise<void>;
export declare const requestDeletePhoto: (req: AuthRequest, res: Response) => Promise<void>;
export declare const approveDeletePhoto: (req: AuthRequest, res: Response) => Promise<void>;
export declare const rejectDeletePhoto: (req: AuthRequest, res: Response) => Promise<void>;
export declare const completeDeletePhoto: (req: AuthRequest, res: Response) => Promise<void>;
export declare const abortDeletePhoto: (req: AuthRequest, res: Response) => Promise<void>;
export declare const deletePhoto: (req: AuthRequest, res: Response) => Promise<void>;
export declare const restorePhoto: (req: AuthRequest, res: Response) => Promise<void>;
export declare const hardDeletePhoto: (req: AuthRequest, res: Response) => Promise<void>;
//# sourceMappingURL=photoController.d.ts.map