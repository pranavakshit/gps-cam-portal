"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const fs_1 = __importDefault(require("fs"));
const path_1 = __importDefault(require("path"));
const lgdImportService_1 = require("../services/lgdImportService");
async function run() {
    const filePath = process.argv[2];
    if (!filePath) {
        console.error('Usage: npx ts-node src/scripts/import-lgd.ts <path-to-zip>');
        process.exit(1);
    }
    const absolutePath = path_1.default.resolve(filePath);
    if (!fs_1.default.existsSync(absolutePath)) {
        console.error(`Error: File not found at ${absolutePath}`);
        process.exit(1);
    }
    console.log(`Starting LGD Import from ${absolutePath}`);
    console.log('Loading file into memory... (This may take a minute for large files)');
    const buffer = fs_1.default.readFileSync(absolutePath);
    console.log('File loaded. Beginning import process...');
    try {
        await (0, lgdImportService_1.processLgdZip)(buffer, (progress, message) => {
            // Output progress to console instead of SSE
            process.stdout.write(`\r[${Math.round(progress)}%] ${message.padEnd(80)}`);
        });
        console.log('\n\n✅ LGD Data successfully imported!');
        process.exit(0);
    }
    catch (err) {
        console.error('\n\n❌ Import failed:', err.message || err);
        process.exit(1);
    }
}
run();
//# sourceMappingURL=import-lgd.js.map