import { Request, Response } from 'express';
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
    
    // The Android app will send metadata
    const { locationName, latitude, longitude, timestamp, uploader } = req.body;

    if (!locationName || !uploader) {
      res.status(400).json({ error: 'locationName and uploader are required' });
      return;
    }

    // Find or create the user based on the offline uploader name
    let user = await prisma.user.findUnique({ where: { username: uploader } });
    if (!user) {
      // Create a dummy user for the offline uploader
      user = await prisma.user.create({
        data: {
          username: uploader,
          password_hash: 'offline_user_no_password',
          role: 'user'
        }
      });
    }

    const userId = user.id;

    // Save each photo to the database
    const createdPhotos = [];
    for (const file of files) {
      const photo = await prisma.photo.create({
        data: {
          imageUrl: `/uploads/${file.filename}`, // Relative path for serving
          locationName,
          userId: userId,
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

export const getPhotos = async (req: Request, res: Response): Promise<void> => {
  try {
    const photos = await prisma.photo.findMany({
      include: {
        user: {
          select: { id: true, username: true } // Don't send password hash
        }
      },
      orderBy: { timestamp: 'desc' }
    });
    
    // Transform the data slightly for the frontend MVP
    const transformed = photos.map(p => ({
      id: p.id,
      locationName: p.locationName,
      latitude: p.latitude,
      longitude: p.longitude,
      timestamp: p.timestamp,
      imageUrl: p.imageUrl,
      uploader: p.user.username
    }));

    res.status(200).json(transformed);
  } catch (error) {
    console.error('Get photos error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
};
