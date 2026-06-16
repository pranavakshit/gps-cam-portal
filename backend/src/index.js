"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = __importDefault(require("express"));
const cors_1 = __importDefault(require("cors"));
const dotenv_1 = __importDefault(require("dotenv"));
const path_1 = __importDefault(require("path"));
const authRoutes_1 = __importDefault(require("./routes/authRoutes"));
const locationRoutes_1 = __importDefault(require("./routes/locationRoutes"));
const photoRoutes_1 = __importDefault(require("./routes/photoRoutes"));
const userRoutes_1 = __importDefault(require("./routes/userRoutes"));
const dockerRoutes_1 = __importDefault(require("./routes/dockerRoutes"));
dotenv_1.default.config();
const app = (0, express_1.default)();
const PORT = process.env.PORT || 5000;
const allowedOrigins = [
    'http://localhost:5173',
    'http://localhost',
    'https://pranavakshit.in',
    'https://www.pranavakshit.in',
    'https://api.pranavakshit.in'
];
app.use((0, cors_1.default)({
    origin: function (origin, callback) {
        // allow requests with no origin (like mobile apps or curl requests)
        if (!origin)
            return callback(null, true);
        if (allowedOrigins.indexOf(origin) === -1) {
            var msg = 'The CORS policy for this site does not allow access from the specified Origin.';
            return callback(new Error(msg), false);
        }
        return callback(null, true);
    }
}));
app.use(express_1.default.json());
app.use('/uploads', express_1.default.static(path_1.default.join(__dirname, '../uploads')));
// Routes
app.use('/api/auth', authRoutes_1.default);
app.use('/api/locations', locationRoutes_1.default);
app.use('/api/photos', photoRoutes_1.default);
app.use('/api/users', userRoutes_1.default);
app.use('/api/docker', dockerRoutes_1.default);
app.get('/', (req, res) => {
    res.send('GPS Cam Portal Backend API is running!');
});
app.get('/health', (req, res) => {
    res.status(200).json({ status: 'ok', message: 'Backend is running' });
});
app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});
//# sourceMappingURL=index.js.map