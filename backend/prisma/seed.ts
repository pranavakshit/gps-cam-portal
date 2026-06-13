import * as dotenv from 'dotenv';
dotenv.config({ path: __dirname + '/../.env' });
import { PrismaClient } from '@prisma/client';
import bcrypt from 'bcryptjs';

const prisma = new PrismaClient();

async function main() {
  const adminUser = process.env.ADMIN_USERNAME || 'pranavakshit';
  const adminPass = process.env.ADMIN_PASSWORD || 't2mji8fwdd';
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

  const viewerUser = process.env.VIEWER_USERNAME || 'viewer';
  const viewerPass = process.env.VIEWER_PASSWORD || 'viewer';
  const viewerHash = await bcrypt.hash(viewerPass, 10);

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
