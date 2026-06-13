import * as dotenv from 'dotenv';
dotenv.config({ path: __dirname + '/../.env' });
import { PrismaClient } from '@prisma/client';
import bcrypt from 'bcryptjs';

const prisma = new PrismaClient();

async function main() {
  const adminUser = process.env.ADMIN_USERNAME || 'admin';
  const adminPass = process.env.ADMIN_PASSWORD || 'admin';
  const passwordHash = await bcrypt.hash(adminPass, 10);
  
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
}

main()
  .catch((e) => {
    console.error(e);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
