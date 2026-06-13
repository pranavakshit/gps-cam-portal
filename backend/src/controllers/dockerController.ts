import { Request, Response } from 'express';
import { exec } from 'child_process';
import util from 'util';

const execPromise = util.promisify(exec);

export const listContainers = async (req: Request, res: Response): Promise<void> => {
  try {
    // We expect the backend to have access to the docker socket
    const { stdout, stderr } = await execPromise('docker ps --format "{{json .}}"');
    
    // Parse the JSON output from docker ps
    const containers = stdout.trim().split('\n').filter(line => line).map(line => JSON.parse(line));
    res.status(200).json(containers);
  } catch (error: any) {
    console.error('Docker list error:', error);
    res.status(500).json({ error: 'Failed to list containers', details: error.message });
  }
};

export const getContainerLogs = async (req: Request, res: Response): Promise<void> => {
  try {
    const { id } = req.params;
    // Tail last 100 lines
    const { stdout, stderr } = await execPromise(`docker logs --tail 100 ${id}`);
    res.status(200).json({ logs: stdout + stderr });
  } catch (error: any) {
    console.error('Docker logs error:', error);
    res.status(500).json({ error: 'Failed to fetch logs', details: error.message });
  }
};

export const restartContainer = async (req: Request, res: Response): Promise<void> => {
  try {
    const { id } = req.params;
    await execPromise(`docker restart ${id}`);
    res.status(200).json({ message: `Container ${id} restarted successfully` });
  } catch (error: any) {
    console.error('Docker restart error:', error);
    res.status(500).json({ error: 'Failed to restart container', details: error.message });
  }
};

export const startContainer = async (req: Request, res: Response): Promise<void> => {
  try {
    const { id } = req.params;
    await execPromise(`docker start ${id}`);
    res.status(200).json({ message: `Container ${id} started successfully` });
  } catch (error: any) {
    console.error('Docker start error:', error);
    res.status(500).json({ error: 'Failed to start container', details: error.message });
  }
};
