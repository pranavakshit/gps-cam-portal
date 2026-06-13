import * as fs from 'fs';
import * as path from 'path';

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
    const token = jwt.sign({ id: 1, role: 'ADMIN' }, 'your-secret-key-here', { expiresIn: '1h' });

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
        if (done) break;
        console.log('Chunk:', decoder.decode(value));
      }
    }
  } catch (err) {
    console.error('Fetch error:', err);
  }
}

testUpload();
