FROM node:22-alpine AS builder

ARG APP_NAME
ARG NEXT_PUBLIC_BASE_PATH=""
ARG NEXT_PUBLIC_API_BASE_URL
ARG NEXT_PUBLIC_API_BASE_PATH=""
ARG NEXT_PUBLIC_WEB_BASE_URL
ARG NEXT_PUBLIC_WEB_BASE_PATH=""
ARG NEXT_PUBLIC_ADMIN_BASE_URL
ARG NEXT_PUBLIC_ADMIN_BASE_PATH

ENV NEXT_TELEMETRY_DISABLED=1 \
    NEXT_PUBLIC_BASE_PATH=${NEXT_PUBLIC_BASE_PATH} \
    NEXT_PUBLIC_API_BASE_URL=${NEXT_PUBLIC_API_BASE_URL} \
    NEXT_PUBLIC_API_BASE_PATH=${NEXT_PUBLIC_API_BASE_PATH} \
    NEXT_PUBLIC_WEB_BASE_URL=${NEXT_PUBLIC_WEB_BASE_URL} \
    NEXT_PUBLIC_WEB_BASE_PATH=${NEXT_PUBLIC_WEB_BASE_PATH} \
    NEXT_PUBLIC_ADMIN_BASE_URL=${NEXT_PUBLIC_ADMIN_BASE_URL} \
    NEXT_PUBLIC_ADMIN_BASE_PATH=${NEXT_PUBLIC_ADMIN_BASE_PATH}

WORKDIR /repo

RUN corepack enable

COPY package.json yarn.lock .yarnrc.yml yarn.config.cjs turbo.json ./
COPY apps ./apps
COPY packages ./packages

RUN --mount=type=cache,target=/usr/local/share/.cache/yarn \
    yarn install --immutable && \
    yarn turbo run build --filter="${APP_NAME}"

FROM node:22-alpine AS runtime

ARG APP_NAME
ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    HOSTNAME=0.0.0.0 \
    PORT=3000 \
    APP_NAME=${APP_NAME}

WORKDIR /app

RUN addgroup --system --gid 10001 syncturtle && \
    adduser --system --uid 10001 --ingroup syncturtle syncturtle

COPY --from=builder --chown=10001:10001 /repo/apps/${APP_NAME}/.next/standalone ./
COPY --from=builder --chown=10001:10001 /repo/apps/${APP_NAME}/public ./apps/${APP_NAME}/public
COPY --from=builder --chown=10001:10001 /repo/apps/${APP_NAME}/.next/static ./apps/${APP_NAME}/.next/static

USER 10001:10001

EXPOSE 3000

ENTRYPOINT ["/bin/sh", "-c", "exec node apps/${APP_NAME}/server.js"]
