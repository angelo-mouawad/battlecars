# -------- Stage 1: Install dependencies --------
FROM node:current-alpine AS base
RUN apk add --no-cache libc6-compat
WORKDIR /app

FROM base AS deps
COPY package*.json ./
RUN npm install

# -------- Stage 2: Build the app --------
FROM base AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .

# The backend API URL — injected at build time
ARG NEXT_PUBLIC_API_URL="http://localhost:4000"
ENV NEXT_PUBLIC_API_URL=${NEXT_PUBLIC_API_URL}

ARG NEXT_PUBLIC_WS_URL="ws://localhost:8080/ws"
ENV NEXT_PUBLIC_WS_URL=${NEXT_PUBLIC_WS_URL}

RUN npm run build

# -------- Stage 3: Production runtime --------
FROM base AS runner
WORKDIR /app

ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

# Fix OKD permissions — give group (OKD's random UID) write access
RUN mkdir -p /app/.next/cache && \
    chgrp -R 0 /app && \
    chmod -R g+rwx /app

# Copy ONLY compiled output from builder stage
COPY --from=builder /app/public ./public
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static

EXPOSE 3000

# Run the compiled server 
USER 1001
CMD ["node", "server.js"]