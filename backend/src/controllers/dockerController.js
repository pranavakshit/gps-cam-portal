"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.startContainer = exports.restartContainer = exports.getContainerLogs = exports.listContainers = void 0;
const child_process_1 = require("child_process");
const util_1 = __importDefault(require("util"));
const execPromise = util_1.default.promisify(child_process_1.exec);
const listContainers = async (req, res) => {
    try {
        // We expect the backend to have access to the docker socket
        const { stdout, stderr } = await execPromise('docker ps --format "{{json .}}"');
        // Parse the JSON output from docker ps
        const containers = stdout.trim().split('\n').filter(line => line).map(line => JSON.parse(line));
        res.status(200).json(containers);
    }
    catch (error) {
        console.error('Docker list error:', error);
        res.status(500).json({ error: 'Failed to list containers', details: error.message });
    }
};
exports.listContainers = listContainers;
const getContainerLogs = async (req, res) => {
    try {
        const { id } = req.params;
        // Tail last 100 lines
        const { stdout, stderr } = await execPromise(`docker logs --tail 100 ${id}`);
        res.status(200).json({ logs: stdout + stderr });
    }
    catch (error) {
        console.error('Docker logs error:', error);
        res.status(500).json({ error: 'Failed to fetch logs', details: error.message });
    }
};
exports.getContainerLogs = getContainerLogs;
const restartContainer = async (req, res) => {
    try {
        const { id } = req.params;
        await execPromise(`docker restart ${id}`);
        res.status(200).json({ message: `Container ${id} restarted successfully` });
    }
    catch (error) {
        console.error('Docker restart error:', error);
        res.status(500).json({ error: 'Failed to restart container', details: error.message });
    }
};
exports.restartContainer = restartContainer;
const startContainer = async (req, res) => {
    try {
        const { id } = req.params;
        await execPromise(`docker start ${id}`);
        res.status(200).json({ message: `Container ${id} started successfully` });
    }
    catch (error) {
        console.error('Docker start error:', error);
        res.status(500).json({ error: 'Failed to start container', details: error.message });
    }
};
exports.startContainer = startContainer;
//# sourceMappingURL=dockerController.js.map