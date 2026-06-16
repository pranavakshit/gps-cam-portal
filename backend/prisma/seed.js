"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const dotenv = __importStar(require("dotenv"));
dotenv.config({ path: __dirname + '/../.env' });
const client_1 = require("@prisma/client");
const bcryptjs_1 = __importDefault(require("bcryptjs"));
const prisma = new client_1.PrismaClient();
async function main() {
    const adminUser = process.env.ADMIN_USERNAME || 'pranavakshit';
    const adminPass = process.env.ADMIN_PASSWORD;
    if (!adminPass) {
        throw new Error('FATAL ERROR: ADMIN_PASSWORD environment variable is missing.');
    }
    const passwordHash = await bcryptjs_1.default.hash(adminPass, 10);
    const admin = await prisma.user.upsert({
        where: { username: adminUser },
        update: {
            password_hash: passwordHash,
            role: 'ADMIN',
        },
        create: {
            username: adminUser,
            password_hash: passwordHash,
            role: 'ADMIN',
        },
    });
    console.log('Seeded database with admin user:', admin.username);
    const viewerUser = process.env.VIEWER_USERNAME || 'viewer';
    const viewerPass = process.env.VIEWER_PASSWORD;
    if (!viewerPass) {
        throw new Error('FATAL ERROR: VIEWER_PASSWORD environment variable is missing.');
    }
    const viewerHash = await bcryptjs_1.default.hash(viewerPass, 10);
    const viewer = await prisma.user.upsert({
        where: { username: viewerUser },
        update: {
            password_hash: viewerHash,
            role: 'VISITOR',
        },
        create: {
            username: viewerUser,
            password_hash: viewerHash,
            role: 'VISITOR',
        },
    });
    console.log('Seeded database with visitor user:', viewer.username);
}
main()
    .catch((e) => {
    console.error(e);
})
    .finally(async () => {
    await prisma.$disconnect();
});
//# sourceMappingURL=seed.js.map