ARG NODE_IMAGE=node:22-alpine
ARG NGINX_IMAGE=nginxinc/nginx-unprivileged:stable-alpine@sha256:782bb89768cc87b7f95c18b95baa1af250e5d451f26794f59363540161147c01
FROM ${NODE_IMAGE} AS build
WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# A slash produces same-origin /api requests without embedding a deployment host.
ARG VITE_API_BASE_URL=/
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run lint && npm run build

FROM ${NGINX_IMAGE}
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY edge-default.conf /etc/nginx/healthmate-edge.conf
COPY --from=build /workspace/dist /usr/share/nginx/html
USER 101:101
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/healthz || exit 1
# Bypass image startup templating: configuration is fixed and rootfs can be read-only.
ENTRYPOINT ["nginx", "-g", "daemon off;"]
