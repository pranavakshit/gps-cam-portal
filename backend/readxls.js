const xlsx = require('xlsx');
const fs = require('fs');

const dir = '../data/extracted/';
const files = fs.readdirSync(dir);

files.forEach(file => {
    if (!file.startsWith("district")) return;
    try {
        const workbook = xlsx.readFile(dir + file);
        const sheetName = workbook.SheetNames[0];
        const sheet = workbook.Sheets[sheetName];
        const data = xlsx.utils.sheet_to_json(sheet, { header: 1 });
        console.log(`\nFile: ${file}`);
        for (let i = 0; i < 5; i++) {
            console.log(`Row ${i}:`, data[i]);
        }
    } catch (e) {
        console.error("Failed to parse " + file, e.message);
    }
});
