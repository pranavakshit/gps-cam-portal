import AdmZip from 'adm-zip';
import * as xlsx from 'xlsx';
import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

function findHeaderRow(data: any[][], keywords: string[]): number {
    if (!data || !Array.isArray(data)) return -1;
    for (let i = 0; i < Math.min(10, data.length || 0); i++) {
        const row = data[i] as any[] | undefined;
        if (!row || !Array.isArray(row)) continue;
        const rowStr = row.join(' ').toLowerCase();
        if (keywords.every(k => rowStr.includes(k.toLowerCase()))) {
            return i;
        }
    }
    return -1;
}

export async function processLgdZip(buffer: Buffer, onProgress?: (progress: number, message: string) => void, isCancelled?: () => boolean) {
    if (onProgress) onProgress(0, 'Reading ZIP archive...');
    const zip = new AdmZip(buffer);
    const zipEntries = zip.getEntries();
    
    zipEntries.sort((a, b) => {
        const getPriority = (name: string) => {
            const lower = name.toLowerCase();
            if (lower.includes('villageofspecificstate')) return 3;
            if (lower.includes('subdistrict')) return 2;
            if (lower.includes('district')) return 1;
            return 4;
        };
        return getPriority(a.name) - getPriority(b.name);
    });
    
    const validEntries = zipEntries.filter(e => {
        const lowerName = e.name.toLowerCase();
        if (!lowerName.endsWith('.xls') && !lowerName.endsWith('.xlsx')) return false;
        return lowerName.includes('villageofspecificstate') || 
               lowerName.includes('subdistrict') || 
               lowerName.includes('district');
    });
    
    let absoluteTotalRows = 0;
    
    // Pass 1: Calculate total absolute rows to display true progress
    for (let i = 0; i < validEntries.length; i++) {
        if (isCancelled && isCancelled()) return;
        const entry = validEntries[i];
        if (!entry) continue;
        if (onProgress) onProgress((i / validEntries.length) * 5, `Analyzing file size: ${entry.name}...`);
        
        const fileBuffer = entry.getData();
        const workbook = xlsx.read(fileBuffer, { type: 'buffer' });
        const sheetName = workbook.SheetNames[0];
        if (!sheetName || !workbook.Sheets[sheetName]) continue;
        const data = xlsx.utils.sheet_to_json<any[]>(workbook.Sheets[sheetName], { header: 1 });
        absoluteTotalRows += data.length;
    }

    let absoluteProcessedRows = 0;
    const startTime = Date.now();

    // Pass 2: Actually process and insert the data
    for (let i = 0; i < validEntries.length; i++) {
        if (isCancelled && isCancelled()) return;
        const entry = validEntries[i];
        if (!entry) continue;
        const entryName = entry.name;
        const lowerName = entryName.toLowerCase();
        
        const fileBuffer = entry.getData();
        const workbook = xlsx.read(fileBuffer, { type: 'buffer' });
        const sheetName = workbook.SheetNames[0];
        if (!sheetName || !workbook.Sheets[sheetName]) continue;
        const data = xlsx.utils.sheet_to_json<any[]>(workbook.Sheets[sheetName], { header: 1 });
        
        const titleText = data[1]?.[0] || '';
        const stateCodeMatch = titleText.match(/State Code\s*:\s*(\d+)/i);
        const stateCode = stateCodeMatch ? parseInt(stateCodeMatch[1]) : 1;
        
        const stateNameMatch = titleText.match(/of\s+(.*?)\(State Code/i);
        if (stateNameMatch) {
            const stateName = stateNameMatch[1].trim();
            await prisma.lgdState.upsert({
                where: { lgdCode: stateCode },
                update: { name: stateName },
                create: { lgdCode: stateCode, name: stateName }
            });
        } else {
            await prisma.lgdState.upsert({
                where: { lgdCode: stateCode },
                update: {},
                create: { lgdCode: stateCode, name: `State ${stateCode}` }
            });
        }

        const reportRowProgress = () => {
            absoluteProcessedRows++;
            if (onProgress && absoluteProcessedRows % 100 === 0) {
                const progress = 5 + (absoluteProcessedRows / absoluteTotalRows) * 95;
                const elapsedSecs = (Date.now() - startTime) / 1000;
                const speed = elapsedSecs > 0 ? Math.round(absoluteProcessedRows / elapsedSecs) : 0;
                const remainingRows = absoluteTotalRows - absoluteProcessedRows;
                const etaSecs = speed > 0 ? Math.round(remainingRows / speed) : 0;
                
                onProgress(
                    Math.min(99, progress), 
                    `Parsing ${entryName} (${absoluteProcessedRows.toLocaleString()} / ${absoluteTotalRows.toLocaleString()}) - ${speed} rows/sec - ETA: ${etaSecs}s`
                );
            }
        };

        if (lowerName.includes('villageofspecificstate')) {
            const headerIdx = findHeaderRow(data, ['village code', 'village name']);
            if (headerIdx !== -1) {
                absoluteProcessedRows += (headerIdx + 2);
                for (let j = headerIdx + 2; j < data.length; j++) {
                    if (isCancelled && isCancelled()) return;
                    reportRowProgress();
                    const row = data[j];
                    if (!row || !row[5] || isNaN(parseInt(row[5]))) continue;
                    
                    const districtCode = parseInt(row[1]);
                    const districtName = row[2] || `District ${districtCode}`;
                    const subDistrictCode = parseInt(row[3]);
                    const subDistrictName = row[4] || `SubDistrict ${subDistrictCode}`;
                    const code = parseInt(row[5]);
                    const name = row[7] || row[6];
                    
                    if (!isNaN(districtCode)) {
                        await prisma.lgdDistrict.upsert({
                            where: { lgdCode: districtCode },
                            update: {},
                            create: { lgdCode: districtCode, name: String(districtName), stateCode }
                        });
                    }

                    if (!isNaN(subDistrictCode)) {
                        await prisma.lgdSubDistrict.upsert({
                            where: { lgdCode: subDistrictCode },
                            update: {},
                            create: { lgdCode: subDistrictCode, name: String(subDistrictName), districtCode }
                        });
                    }
                    
                    await prisma.lgdVillage.upsert({
                        where: { lgdCode: code },
                        update: { name: String(name), subDistrictCode },
                        create: { lgdCode: code, name: String(name), subDistrictCode }
                    });
                }
            } else {
                absoluteProcessedRows += data.length;
            }
        }
        else if (lowerName.includes('subdistrict')) {
            const headerIdx = findHeaderRow(data, ['subdistrict code', 'subdistrict name']);
            if (headerIdx !== -1) {
                absoluteProcessedRows += (headerIdx + 2);
                for (let j = headerIdx + 2; j < data.length; j++) {
                    if (isCancelled && isCancelled()) return;
                    reportRowProgress();
                    const row = data[j];
                    if (!row || !row[3] || isNaN(parseInt(row[3]))) continue;
                    
                    const districtCode = parseInt(row[1]);
                    const districtName = row[2] || `District ${districtCode}`;
                    const code = parseInt(row[3]);
                    const name = row[5] || row[4];
                    
                    if (!isNaN(districtCode)) {
                        await prisma.lgdDistrict.upsert({
                            where: { lgdCode: districtCode },
                            update: {},
                            create: { lgdCode: districtCode, name: String(districtName), stateCode }
                        });
                    }

                    await prisma.lgdSubDistrict.upsert({
                        where: { lgdCode: code },
                        update: { name: String(name), districtCode },
                        create: { lgdCode: code, name: String(name), districtCode }
                    });
                }
            } else {
                absoluteProcessedRows += data.length;
            }
        }
        else if (lowerName.includes('district')) {
            const headerIdx = findHeaderRow(data, ['district code', 'district name']);
            if (headerIdx !== -1) {
                absoluteProcessedRows += (headerIdx + 2);
                for (let j = headerIdx + 2; j < data.length; j++) {
                    if (isCancelled && isCancelled()) return;
                    reportRowProgress();
                    const row = data[j];
                    if (!row || !row[1] || isNaN(parseInt(row[1]))) continue;
                    
                    const code = parseInt(row[1]);
                    const name = row[3] || row[2]; 
                    
                    await prisma.lgdDistrict.upsert({
                        where: { lgdCode: code },
                        update: { name: String(name), stateCode },
                        create: { lgdCode: code, name: String(name), stateCode }
                    });
                }
            } else {
                absoluteProcessedRows += data.length;
            }
        } else {
            absoluteProcessedRows += data.length;
        }
    }
    
    if (onProgress) onProgress(100, 'Database sync complete.');
}
