# Build stage
FROM node:18-alpine AS builder
WORKDIR /app

COPY package*.json ./
RUN npm install

COPY tsconfig.json ./
COPY src ./src
RUN npm run build

# Production stage
FROM node:18-alpine
WORKDIR /app

# Install PM2 globally
RUN npm install -g pm2

# Copy built files and dependencies
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/node_modules ./node_modules
COPY package*.json ./
COPY ecosystem.config.js ./

# Create logs directory
RUN mkdir -p logs

# Environment variables
# Paper reference (Section 3.3):
# "UV_THREADPOOL_SIZE=128 - I/O thread pool expansion"
ENV NODE_ENV=production
ENV UV_THREADPOOL_SIZE=128

EXPOSE 3000

# For single instance in Docker, use node directly
# For production with multiple instances, use PM2
CMD ["node", "dist/server.js"]
