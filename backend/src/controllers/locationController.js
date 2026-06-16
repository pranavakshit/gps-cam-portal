"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.deleteLocation = exports.updateLocation = exports.createLocation = exports.getOfflineBundle = exports.searchLocations = exports.getWards = exports.getUlbs = exports.getVillages = exports.getSubDistricts = exports.getDistricts = exports.getStates = exports.importLgdData = void 0;
const prisma_1 = __importDefault(require("../db/prisma"));
const lgdImportService_1 = require("../services/lgdImportService");
const fs_1 = __importDefault(require("fs"));
const importLgdData = async (req, res) => {
    res.setHeader('Content-Type', 'text/event-stream');
    res.setHeader('Cache-Control', 'no-cache');
    res.setHeader('Connection', 'keep-alive');
    res.flushHeaders();
    try {
        if (!req.file) {
            res.write(`data: ${JSON.stringify({ error: 'No ZIP file uploaded' })}\n\n`);
            res.end();
            return;
        }
        let cancelled = false;
        req.on('close', () => {
            cancelled = true;
        });
        await (0, lgdImportService_1.processLgdZip)(fs_1.default.readFileSync(req.file.path), (progress, message) => {
            if (!res.writableEnded) {
                res.write(`data: ${JSON.stringify({ progress, message })}\n\n`);
            }
        }, () => cancelled);
        // Clean up temporary file
        if (fs_1.default.existsSync(req.file.path)) {
            fs_1.default.unlinkSync(req.file.path);
        }
        if (!res.writableEnded) {
            if (cancelled) {
                res.write(`data: ${JSON.stringify({ error: 'Sync cancelled by user.' })}\n\n`);
            }
            else {
                res.write(`data: ${JSON.stringify({ progress: 100, message: 'Done', done: true })}\n\n`);
            }
            res.end();
        }
    }
    catch (error) {
        console.error('LGD import error:', error);
        res.write(`data: ${JSON.stringify({ error: 'Failed to import LGD data: ' + error.message })}\n\n`);
        res.end();
    }
};
exports.importLgdData = importLgdData;
const getStates = async (req, res) => {
    try {
        const states = await prisma_1.default.lgdState.findMany({ orderBy: { name: 'asc' } });
        res.status(200).json(states);
    }
    catch (error) {
        res.status(500).json({ error: 'Internal server error' });
    }
};
exports.getStates = getStates;
const getDistricts = async (req, res) => {
    try {
        const { id } = req.params; // stateCode
        const districts = await prisma_1.default.lgdDistrict.findMany({
            where: { stateCode: Number(id) },
            orderBy: { name: 'asc' }
        });
        res.status(200).json(districts);
    }
    catch (error) {
        res.status(500).json({ error: 'Internal server error' });
    }
};
exports.getDistricts = getDistricts;
const getSubDistricts = async (req, res) => {
    try {
        const { id } = req.params; // districtCode
        const subDistricts = await prisma_1.default.lgdSubDistrict.findMany({
            where: { districtCode: Number(id) },
            orderBy: { name: 'asc' }
        });
        res.status(200).json(subDistricts);
    }
    catch (error) {
        res.status(500).json({ error: 'Internal server error' });
    }
};
exports.getSubDistricts = getSubDistricts;
const getVillages = async (req, res) => {
    try {
        const { id } = req.params; // subDistrictCode
        const villages = await prisma_1.default.lgdVillage.findMany({
            where: { subDistrictCode: Number(id) },
            orderBy: { name: 'asc' }
        });
        res.status(200).json(villages);
    }
    catch (error) {
        res.status(500).json({ error: 'Internal server error' });
    }
};
exports.getVillages = getVillages;
const getUlbs = async (req, res) => {
    try {
        const { id } = req.params; // districtCode
        const ulbs = await prisma_1.default.lgdUlb.findMany({
            where: { districtCode: Number(id) },
            orderBy: { name: 'asc' }
        });
        res.status(200).json(ulbs);
    }
    catch (error) {
        res.status(500).json({ error: 'Internal server error' });
    }
};
exports.getUlbs = getUlbs;
const getWards = async (req, res) => {
    try {
        const { id } = req.params; // ulbCode
        const wards = await prisma_1.default.lgdWard.findMany({
            where: { ulbCode: Number(id) },
            orderBy: { name: 'asc' }
        });
        res.status(200).json(wards);
    }
    catch (error) {
        res.status(500).json({ error: 'Internal server error' });
    }
};
exports.getWards = getWards;
// --- CRUD ENDPOINTS ---
const searchLocations = async (req, res) => {
    try {
        const query = String(req.query.q || '');
        const stateCodeParam = req.query.stateCode ? parseInt(String(req.query.stateCode)) : undefined;
        if (!query || query.length < 2) {
            res.status(200).json([]);
            return;
        }
        // We want to return an array of matching locations with their type and parent info
        const stateWhere = stateCodeParam ? { lgdCode: stateCodeParam } : {};
        const districtWhere = stateCodeParam ? { stateCode: stateCodeParam } : {};
        const subDistrictWhere = stateCodeParam ? { district: { stateCode: stateCodeParam } } : {};
        const villageWhere = stateCodeParam ? { subDistrict: { district: { stateCode: stateCodeParam } } } : {};
        const ulbWhere = stateCodeParam ? { district: { stateCode: stateCodeParam } } : {};
        const wardWhere = stateCodeParam ? { ulb: { district: { stateCode: stateCodeParam } } } : {};
        const states = await prisma_1.default.lgdState.findMany({ where: { name: { contains: query }, ...stateWhere }, take: 10 });
        const districts = await prisma_1.default.lgdDistrict.findMany({ where: { name: { contains: query }, ...districtWhere }, take: 10, include: { state: true } });
        const subDistricts = await prisma_1.default.lgdSubDistrict.findMany({ where: { name: { contains: query }, ...subDistrictWhere }, take: 10, include: { district: { include: { state: true } } } });
        const villages = await prisma_1.default.lgdVillage.findMany({ where: { name: { contains: query }, ...villageWhere }, take: 10, include: { subDistrict: { include: { district: { include: { state: true } } } } } });
        const ulbs = await prisma_1.default.lgdUlb.findMany({ where: { name: { contains: query }, ...ulbWhere }, take: 10, include: { district: { include: { state: true } } } });
        const wards = await prisma_1.default.lgdWard.findMany({ where: { name: { contains: query }, ...wardWhere }, take: 10, include: { ulb: { include: { district: { include: { state: true } } } } } });
        const results = [
            ...states.map((s) => ({ id: s.id, lgdCode: s.lgdCode, name: s.name, type: 'State', path: s.name })),
            ...districts.map((d) => ({ id: d.id, lgdCode: d.lgdCode, name: d.name, type: 'District', path: `${d.state.name} > ${d.name}` })),
            ...subDistricts.map((s) => ({ id: s.id, lgdCode: s.lgdCode, name: s.name, type: 'SubDistrict', path: `${s.district.state.name} > ${s.district.name} > ${s.name}`, districtId: s.district.id, stateId: s.district.state.id })),
            ...villages.map((v) => ({ id: v.id, lgdCode: v.lgdCode, name: v.name, type: 'Village', path: `${v.subDistrict.district.state.name} > ${v.subDistrict.district.name} > ${v.subDistrict.name} > ${v.name}`, subDistrictId: v.subDistrict.id, districtId: v.subDistrict.district.id, stateId: v.subDistrict.district.state.id })),
            ...ulbs.map((u) => ({ id: u.id, lgdCode: u.lgdCode, name: u.name, type: 'ULB', path: `${u.district.state.name} > ${u.district.name} > ${u.name}`, districtId: u.district.id, stateId: u.district.state.id })),
            ...wards.map((w) => ({ id: w.id, lgdCode: w.lgdCode, name: w.name, type: 'Ward', path: `${w.ulb.district.state.name} > ${w.ulb.district.name} > ${w.ulb.name} > ${w.name}`, ulbId: w.ulb.id, districtId: w.ulb.district.id, stateId: w.ulb.district.state.id })),
        ];
        // Sort by type priority (States first, then Districts, etc)
        const typePriority = { State: 1, District: 2, SubDistrict: 3, ULB: 3, Village: 4, Ward: 4 };
        results.sort((a, b) => (typePriority[a.type] || 99) - (typePriority[b.type] || 99));
        res.status(200).json(results.slice(0, 20));
    }
    catch (error) {
        res.status(500).json({ error: 'Internal server error' });
    }
};
exports.searchLocations = searchLocations;
const getOfflineBundle = async (req, res) => {
    try {
        const { id } = req.params; // stateCode
        const stateCode = Number(id);
        const state = await prisma_1.default.lgdState.findUnique({ where: { lgdCode: stateCode } });
        if (!state) {
            res.status(404).json({ error: 'State not found' });
            return;
        }
        const districts = await prisma_1.default.lgdDistrict.findMany({ where: { stateCode }, orderBy: { name: 'asc' } });
        const subDistricts = await prisma_1.default.lgdSubDistrict.findMany({ where: { district: { stateCode } }, orderBy: { name: 'asc' } });
        const villages = await prisma_1.default.lgdVillage.findMany({ where: { subDistrict: { district: { stateCode } } }, orderBy: { name: 'asc' } });
        res.status(200).json({
            state,
            districts,
            subDistricts,
            villages
        });
    }
    catch (error) {
        res.status(500).json({ error: 'Internal server error' });
    }
};
exports.getOfflineBundle = getOfflineBundle;
const createLocation = async (req, res) => {
    try {
        const { type, name, lgdCode, parentCode } = req.body;
        let result;
        switch (type) {
            case 'State':
                result = await prisma_1.default.lgdState.create({ data: { lgdCode, name } });
                break;
            case 'District':
                result = await prisma_1.default.lgdDistrict.create({ data: { lgdCode, name, stateCode: parentCode } });
                break;
            case 'SubDistrict':
                result = await prisma_1.default.lgdSubDistrict.create({ data: { lgdCode, name, districtCode: parentCode } });
                break;
            case 'Village':
                result = await prisma_1.default.lgdVillage.create({ data: { lgdCode, name, subDistrictCode: parentCode } });
                break;
            case 'ULB':
                result = await prisma_1.default.lgdUlb.create({ data: { lgdCode, name, districtCode: parentCode } });
                break;
            case 'Ward':
                result = await prisma_1.default.lgdWard.create({ data: { lgdCode, name, ulbCode: parentCode } });
                break;
            default:
                res.status(400).json({ error: 'Invalid location type' });
                return;
        }
        res.status(201).json(result);
    }
    catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to create location' });
    }
};
exports.createLocation = createLocation;
const updateLocation = async (req, res) => {
    try {
        const { type, name } = req.body;
        const lgdCode = Number(req.params.id);
        let result;
        switch (type) {
            case 'State':
                result = await prisma_1.default.lgdState.update({ where: { lgdCode }, data: { name } });
                break;
            case 'District':
                result = await prisma_1.default.lgdDistrict.update({ where: { lgdCode }, data: { name } });
                break;
            case 'SubDistrict':
                result = await prisma_1.default.lgdSubDistrict.update({ where: { lgdCode }, data: { name } });
                break;
            case 'Village':
                result = await prisma_1.default.lgdVillage.update({ where: { lgdCode }, data: { name } });
                break;
            case 'ULB':
                result = await prisma_1.default.lgdUlb.update({ where: { lgdCode }, data: { name } });
                break;
            case 'Ward':
                result = await prisma_1.default.lgdWard.update({ where: { lgdCode }, data: { name } });
                break;
            default:
                res.status(400).json({ error: 'Invalid location type' });
                return;
        }
        res.status(200).json(result);
    }
    catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to update location' });
    }
};
exports.updateLocation = updateLocation;
const deleteLocation = async (req, res) => {
    try {
        const type = String(req.query.type);
        const lgdCode = Number(req.params.id);
        switch (type) {
            case 'State':
                await prisma_1.default.lgdState.delete({ where: { lgdCode } });
                break;
            case 'District':
                await prisma_1.default.lgdDistrict.delete({ where: { lgdCode } });
                break;
            case 'SubDistrict':
                await prisma_1.default.lgdSubDistrict.delete({ where: { lgdCode } });
                break;
            case 'Village':
                await prisma_1.default.lgdVillage.delete({ where: { lgdCode } });
                break;
            case 'ULB':
                await prisma_1.default.lgdUlb.delete({ where: { lgdCode } });
                break;
            case 'Ward':
                await prisma_1.default.lgdWard.delete({ where: { lgdCode } });
                break;
            default:
                res.status(400).json({ error: 'Invalid location type' });
                return;
        }
        res.status(200).json({ success: true });
    }
    catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to delete location' });
    }
};
exports.deleteLocation = deleteLocation;
//# sourceMappingURL=locationController.js.map