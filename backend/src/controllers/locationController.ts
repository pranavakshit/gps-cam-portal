import { Request, Response } from 'express';
import prisma from '../db/prisma';

export const getLocations = async (req: Request, res: Response): Promise<void> => {
  try {
    const locations = await prisma.location.findMany({
      orderBy: { state: 'asc' }
    });
    res.status(200).json(locations);
  } catch (error) {
    console.error('Get locations error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
};

export const createLocation = async (req: Request, res: Response): Promise<void> => {
  try {
    const { state, district, area } = req.body;

    if (!area || !state || !district) {
      res.status(400).json({ error: 'Area, state, and district are required' });
      return;
    }

    const location = await prisma.location.create({
      data: {
        state,
        district,
        area
      }
    });

    res.status(201).json(location);
  } catch (error) {
    console.error('Create location error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
};
