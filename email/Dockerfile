FROM node:18-alpine

WORKDIR /app

# Clear npm cache and ensure clean install
RUN npm cache clean --force

COPY package.json package-lock.json ./

# Use npm ci for reliable, reproducible builds
# Remove node_modules if it exists to ensure clean install
RUN rm -rf node_modules && \
    npm ci --omit=dev && \
    npm cache clean --force

COPY . .

CMD ["npm", "start"]
