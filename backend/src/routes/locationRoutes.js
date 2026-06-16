"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = require("express");
const multer_1 = __importDefault(require("multer"));
const locationController_1 = require("../controllers/locationController");
const authMiddleware_1 = require("../middleware/authMiddleware");
const router = (0, express_1.Router)();
const fs_1 = __importDefault(require("fs"));
const path_1 = __importDefault(require("path"));
// Ensure temp directory exists
const tempDir = path_1.default.join(__dirname, '../../uploads/temp');
if (!fs_1.default.existsSync(tempDir)) {
    fs_1.default.mkdirSync(tempDir, { recursive: true });
}
const storage = multer_1.default.diskStorage({
    destination: function (req, file, cb) {
        cb(null, tempDir);
    },
    filename: function (req, file, cb) {
        cb(null, 'lgd-upload-' + Date.now() + '.zip');
    }
});
const upload = (0, multer_1.default)({ storage: storage });
// Public endpoints for Android App
router.get('/states', locationController_1.getStates);
router.get('/states/:id/districts', locationController_1.getDistricts);
router.get('/districts/:id/subdistricts', locationController_1.getSubDistricts);
router.get('/subdistricts/:id/villages', locationController_1.getVillages);
router.get('/districts/:id/ulbs', locationController_1.getUlbs);
router.get('/ulbs/:id/wards', locationController_1.getWards);
router.get('/search', locationController_1.searchLocations);
router.get('/states/:id/offline-bundle', locationController_1.getOfflineBundle);
// Protected Admin Endpoints
router.post('/import', authMiddleware_1.authenticateJWT, upload.single('zipfile'), locationController_1.importLgdData);
// CRUD Endpoints for Drill-Down Editor
router.get('/search', authMiddleware_1.authenticateJWT, locationController_1.searchLocations);
router.post('/', authMiddleware_1.authenticateJWT, locationController_1.createLocation);
router.put('/:id', authMiddleware_1.authenticateJWT, locationController_1.updateLocation);
router.delete('/:id', authMiddleware_1.authenticateJWT, locationController_1.deleteLocation);
exports.default = router;
//# sourceMappingURL=locationRoutes.js.map