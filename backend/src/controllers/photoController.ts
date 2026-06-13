import { Request, Response } from 'express';
import { AuthRequest } from '../middleware/authMiddleware';
import prisma from '../db/prisma';
import multer from 'multer';
import path from 'path';
import fs from 'fs';

// Configure Multer for local storage
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    const uploadPath = path.join(__dirname, '../../uploads');
    if (!fs.existsSync(uploadPath)) {
      fs.mkdirSync(uploadPath, { recursive: true });
    }
    cb(null, uploadPath);
  },
  filename: (req, file, cb) => {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    cb(null, uniqueSuffix + path.extname(file.originalname));
  }
});

export const upload = multer({ 
  storage,
  limits: { fileSize: 10 * 1024 * 1024 } // 10MB limit
});

export const uploadPhoto = async (req: Request, res: Response): Promise<void> => {
  try {
    if (!req.files || (req.files as Express.Multer.File[]).length === 0) {
      res.status(400).json({ error: 'No files uploaded' });
      return;
    }

    const files = req.files as Express.Multer.File[];
    
    // The Android app will send LGD codes now instead of locationName
    const { 
        stateCode, districtCode, subdistrictCode, villageCode, ulbCode, wardCode,
        locationName,
        latitude, longitude, timestamp, uploader 
    } = req.body;
    console.log('Incoming Photo Upload Body:', req.body);

    if (!uploader) {
      res.status(400).json({ error: 'uploader is required' });
      return;
    }

    // Find or create the user based on the offline uploader name
    let user = await prisma.user.findUnique({ where: { username: uploader } });
    if (!user) {
      user = await prisma.user.create({
        data: {
          username: uploader,
          password_hash: 'offline_user_no_password',
          role: 'user'
        }
      });
    }

    if (user.role === 'VISITOR') {
      res.status(403).json({ error: 'Visitor accounts are not permitted to upload photos' });
      return;
    }

    const userId = user.id;

    const createdPhotos = [];
    for (const file of files) {
      const photo = await prisma.photo.create({
        data: {
          imageUrl: `/uploads/${file.filename}`, 
          userId: userId,
          stateCode: stateCode ? parseInt(stateCode) : null,
          districtCode: districtCode ? parseInt(districtCode) : null,
          subdistrictCode: subdistrictCode ? parseInt(subdistrictCode) : null,
          villageCode: villageCode ? parseInt(villageCode) : null,
          ulbCode: ulbCode ? parseInt(ulbCode) : null,
          wardCode: wardCode ? parseInt(wardCode) : null,
          locationName: locationName || null,
          latitude: latitude ? parseFloat(latitude) : 0,
          longitude: longitude ? parseFloat(longitude) : 0,
          timestamp: timestamp ? new Date(timestamp) : new Date(),
        }
      });
      createdPhotos.push(photo);
    }

    res.status(201).json({ message: 'Photos uploaded successfully', photos: createdPhotos });
  } catch (error) {
    console.error('Upload photo error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
};

export const getPhotos = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const userRole = req.user?.role;
    const userId = req.user?.id;
    const isRecycleBin = req.query.recycle_bin === 'true';
    
    let whereClause: any = {};
    if (userRole !== 'ADMIN') {
        whereClause = { userId: userId };
    }
    
    if (isRecycleBin) {
      whereClause.deletionStatus = 'DELETED_SOFT';
    } else {
      whereClause.deletionStatus = { not: 'DELETED_SOFT' };
    }

    const photos = await prisma.photo.findMany({
      where: whereClause,
      include: {
        user: {
          select: { id: true, username: true }
        }
      },
      orderBy: { timestamp: 'desc' }
    });
    
    // Transform the data
    const transformed = photos.map(p => {
        // Construct a string to describe the location based on provided codes
        const locParts = [];
        if (p.villageCode) locParts.push(`Village ${p.villageCode}`);
        if (p.wardCode) locParts.push(`Ward ${p.wardCode}`);
        if (p.subdistrictCode) locParts.push(`SubDist ${p.subdistrictCode}`);
        if (p.ulbCode) locParts.push(`ULB ${p.ulbCode}`);
        if (p.districtCode) locParts.push(`Dist ${p.districtCode}`);
        if (p.stateCode) locParts.push(`State ${p.stateCode}`);
        
        const locationStr = p.locationName ? p.locationName : (locParts.length > 0 ? locParts.join(', ') : 'Unknown Location');

        return {
          id: p.id,
          locationName: locationStr, // Providing a formatted string for frontend backwards compatibility
          stateCode: p.stateCode,
          districtCode: p.districtCode,
          latitude: p.latitude,
          longitude: p.longitude,
          timestamp: p.timestamp,
          imageUrl: p.imageUrl,
          uploader: p.user.username,
          deletionStatus: p.deletionStatus,
          deletionReason: p.deletionReason
        };
    });

    res.status(200).json(transformed);
  } catch (error) {
    console.error('Get photos error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
};

export const requestDeletePhoto = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const { id } = req.params;
    const { reason } = req.body;
    
    const photo = await prisma.photo.findUnique({ where: { id: Number(id) } });
    if (!photo) {
      res.status(404).json({ error: 'Photo not found' });
      return;
    }

    const isAdmin = req.user?.role === 'ADMIN';
    const isOwner = req.user?.id === photo.userId;

    if (!isAdmin && !isOwner) {
      res.status(403).json({ error: 'Unauthorized' });
      return;
    }

    const newStatus = isAdmin ? 'ADMIN_REQUESTED' : 'USER_REQUESTED';
    
    const updated = await prisma.photo.update({
      where: { id: Number(id) },
      data: {
        deletionStatus: newStatus,
        deletionReason: reason || null
      }
    });

    res.status(200).json({ message: 'Deletion requested successfully', photo: updated });
  } catch (error) {
    console.error('Request delete error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
};

export const approveDeletePhoto = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const { id } = req.params;
    
    if (req.user?.role !== 'ADMIN') {
      res.status(403).json({ error: 'Only admins can approve deletion requests' });
      return;
    }

    const photo = await prisma.photo.findUnique({ where: { id: Number(id) } });
    if (!photo) {
      res.status(404).json({ error: 'Photo not found' });
      return;
    }

    if (photo.deletionStatus !== 'USER_REQUESTED') {
      res.status(400).json({ error: 'Photo is not in USER_REQUESTED state' });
      return;
    }

    const updated = await prisma.photo.update({
      where: { id: Number(id) },
      data: { deletionStatus: 'ADMIN_APPROVED' }
    });

    res.status(200).json({ message: 'Deletion approved successfully', photo: updated });
  } catch (error) {
    console.error('Approve delete error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
};

export const deletePhoto = async (req: AuthRequest, res: Response): Promise<void> => {
  try {
    const { id } = req.params;
    
    const photo = await prisma.photo.findUnique({
      where: { id: Number(id) }
    });
    
    if (!photo) {
      res.status(404).json({ error: 'Photo not found' });
      return;
    }
    
    const isAdmin = req.user?.role === 'ADMIN';
    const isOwner = req.user?.id === photo.userId;

    if (!isAdmin && !isOwner) {
      res.status(403).json({ error: 'Unauthorized to delete this photo' });
      return;
    }
    
    if (!isAdmin && photo.deletionStatus !== 'ADMIN_APPROVED' && photo.deletionStatus !== 'ADMIN_REQUESTED') {
      res.status(400).json({ error: 'Deletion must be approved by an admin first' });
      return;
    }
    
    // Soft Delete
    await prisma.photo.update({
      where: { id: Number(id) },
      data: { deletionStatus: 'DELETED_SOFT' }
    });
    
    res.status(200).json({ message: 'Photo softly deleted successfully' });
  } catch (error) {
    console.error('Delete photo error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
};
