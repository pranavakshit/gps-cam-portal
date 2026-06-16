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
Object.defineProperty(exports, "__esModule", { value: true });
const fs = __importStar(require("fs"));
const path = __importStar(require("path"));
async function testUpload() {
    const filePath = path.resolve('../data/downloadDir2026_06_13_13_31_59_130.zip');
    if (!fs.existsSync(filePath)) {
        console.error('File not found at', filePath);
        return;
    }
    const buffer = fs.readFileSync(filePath);
    const boundary = '----WebKitFormBoundary7MA4YWxkTrZu0gW';
    let body = '';
    body += `--${boundary}\r\n`;
    body += `Content-Disposition: form-data; name="zipfile"; filename="test.zip"\r\n`;
    body += `Content-Type: application/zip\r\n\r\n`;
    const payload = Buffer.concat([
        Buffer.from(body, 'utf-8'),
        buffer,
        Buffer.from(`\r\n--${boundary}--\r\n`, 'utf-8')
    ]);
    try {
        const jwt = require('jsonwebtoken');
        const JWT_SECRET = process.env.JWT_SECRET;
        if (!JWT_SECRET) {
            console.error('FATAL ERROR: JWT_SECRET environment variable is missing.');
            process.exit(1);
        }
        const token = jwt.sign({ id: 1, role: 'ADMIN' }, JWT_SECRET, { expiresIn: '1h' });
        console.log('Sending request...');
        const res = await fetch('http://localhost:5000/api/locations/import', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': `multipart/form-data; boundary=${boundary}`
            },
            body: payload
        });
        console.log('Status:', res.status);
        console.log('Headers:', res.headers);
        if (res.body) {
            const reader = res.body.getReader();
            const decoder = new TextDecoder();
            while (true) {
                const { done, value } = await reader.read();
                if (done)
                    break;
                console.log('Chunk:', decoder.decode(value));
            }
        }
    }
    catch (err) {
        console.error('Fetch error:', err);
    }
}
testUpload();
//# sourceMappingURL=test-upload.js.map