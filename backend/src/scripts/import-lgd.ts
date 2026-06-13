import fs from 'fs';
import path from 'path';
import { processLgdZip } from '../services/lgdImportService';

async function run() {
  const filePath = process.argv[2];
  
  if (!filePath) {
    console.error('Usage: npx ts-node src/scripts/import-lgd.ts <path-to-zip>');
    process.exit(1);
  }

  const absolutePath = path.resolve(filePath);
  if (!fs.existsSync(absolutePath)) {
    console.error(`Error: File not found at ${absolutePath}`);
    process.exit(1);
  }

  console.log(`Starting LGD Import from ${absolutePath}`);
  console.log('Loading file into memory... (This may take a minute for large files)');
  
  const buffer = fs.readFileSync(absolutePath);
  
  console.log('File loaded. Beginning import process...');

  try {
    await processLgdZip(
      buffer,
      (progress, message) => {
        // Output progress to console instead of SSE
        process.stdout.write(`\r[${Math.round(progress)}%] ${message.padEnd(80)}`);
      }
    );
    console.log('\n\n✅ LGD Data successfully imported!');
    process.exit(0);
  } catch (err: any) {
    console.error('\n\n❌ Import failed:', err.message || err);
    process.exit(1);
  }
}

run();
