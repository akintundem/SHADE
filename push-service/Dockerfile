FROM node:18-alpine
WORKDIR /app

COPY package.json package-lock.json* ./
RUN if [ -f package-lock.json ]; then npm ci --omit=dev; else npm install --omit=dev; fi

# Copy application files (excluding cred folder for security)
COPY server.js ./
COPY .env* ./

CMD ["npm", "run", "start"]
