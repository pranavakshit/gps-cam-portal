import * as fs from 'fs';
import { PrismaClient } from '@prisma/client';
import { processLgdZip } from './src/services/lgdImportService';

const prisma = new PrismaClient();

async function run() {
    console.log('Importing correct Ladakh data...');
    const buf = fs.readFileSync('../data/downloadDir2026_06_13_13_31_59_130.zip');
    await processLgdZip(buf);
    console.log('Done');
}

run().catch(console.error).finally(() => prisma.$disconnect());
