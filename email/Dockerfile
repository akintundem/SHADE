# --- Build stage ---
FROM node:18-alpine AS builder

WORKDIR /app

COPY package.json package-lock.json ./

# Install all deps (including devDeps needed for tsc)
RUN npm ci && npm cache clean --force

COPY . .

# Compile TypeScript to dist/
RUN npm run build

# --- Runtime stage ---
FROM node:18-alpine

WORKDIR /app

COPY package.json package-lock.json ./

# Install production deps only
RUN npm ci --omit=dev && npm cache clean --force

# Copy compiled output from builder
COPY --from=builder /app/dist ./dist

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

CMD ["node", "dist/server.js"]
