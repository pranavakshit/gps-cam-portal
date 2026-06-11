FROM node:20-alpine

WORKDIR /app

# Copy package files and install dependencies
COPY package*.json ./
COPY tsconfig.json ./
COPY prisma ./prisma/

RUN npm install

# Copy source code
COPY src ./src

# Generate Prisma client
RUN npx prisma generate

# Build TypeScript code (if we add a build step later, for now we run ts-node)
# RUN npm run build

EXPOSE 5000

CMD ["npm", "run", "start"]
