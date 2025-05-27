FROM node:24-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install --no-cache
COPY . .
RUN npm run build

FROM nginx:alpine
RUN rm -rf /etc/nginx/conf.d/*
COPY --from=builder /app/out /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Set proper permissions for nginx user
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chmod -R 755 /usr/share/nginx/html

# ENV NGINX_ENTRYPOINT_QUIET_LOGS=1
RUN echo "daemon off;" >> /etc/nginx/nginx.conf

EXPOSE 80
CMD ["nginx"]
