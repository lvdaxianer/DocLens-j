ARG DOCLENS_RUNTIME_BASE_IMAGE=doclens:base-amd64

FROM ${DOCLENS_RUNTIME_BASE_IMAGE} AS runtime
USER root
ARG LOCAL_SERVER_DIST_ARCHIVE=docker/build/doclens-server-dist.tar.gz

ENV APP_HOME=/opt/doclens

# `doclens-server-dist.tar.gz` 由 Maven Assembly 生成，里面包含：
# - 后端 Spring Boot jar 和启动脚本
# - 随包分发的配置示例
# - 前端构建产物，它们先由 `doclens-dashboard` 构建到
#   `doclens-server/src/main/resources/static/dashboard`，再随 jar 一起进入镜像
COPY ${LOCAL_SERVER_DIST_ARCHIVE} /tmp/doclens-server-dist.tar.gz
COPY docker/entrypoint.sh /usr/local/bin/doclens-all-in-one-entrypoint.sh

# 解压后的 `/opt/doclens` 就是容器内后端运行目录；启动时会先拉起
# PostgreSQL，再由 `doclens-server.sh` 启动后端，后端继续对外提供 `/dashboard/`。
RUN mkdir -p "${APP_HOME}" \
    && tar -xzf /tmp/doclens-server-dist.tar.gz --strip-components=1 -C "${APP_HOME}" \
    && rm -rf /tmp/doclens-server-dist.tar.gz \
    && chmod +x "${APP_HOME}/bin/doclens-server.sh" /usr/local/bin/doclens-all-in-one-entrypoint.sh \
    && chown -R postgres:postgres "${APP_HOME}"

EXPOSE 10003 5432
VOLUME ["/var/lib/postgresql/data", "/var/lib/doclens/storage", "/opt/doclens/config"]

ENTRYPOINT ["doclens-all-in-one-entrypoint.sh"]
