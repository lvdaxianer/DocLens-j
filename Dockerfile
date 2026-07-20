# 允许调用方覆盖 runtime 基础镜像；默认使用本地构建的 amd64 基础镜像。
ARG DOCLENS_RUNTIME_BASE_IMAGE=doclens:base-amd64

# 使用 runtime 基础镜像作为最终的 DocLens 应用镜像基础层。
FROM ${DOCLENS_RUNTIME_BASE_IMAGE} AS runtime

# 切换到 root，确保后续解压、权限设置和入口脚本安装有足够权限。
USER root

# 指定 Maven Assembly 生成的后端发行包路径；该包同时携带已经打进 jar 的前端资源。
ARG LOCAL_SERVER_DIST_ARCHIVE=docker/build/doclens-server-dist.tar.gz

# 统一定义容器内 DocLens 的应用安装目录。
ENV APP_HOME=/opt/doclens

# scripts/prepare-docker-build-context.sh 会先构建前端、再执行 Maven Assembly，最终生成该归档。
# 归档内容包括后端 Spring Boot jar、启动脚本、配置示例和 static/dashboard 前端资源。
# 将 Assembly 归档复制进临时目录，后面的 RUN 指令会把它解压到 APP_HOME。
COPY ${LOCAL_SERVER_DIST_ARCHIVE} /tmp/doclens-server-dist.tar.gz

# 复制一体化容器入口脚本；它负责依次启动 PostgreSQL 和 DocLens 后端。
COPY docker/entrypoint.sh /usr/local/bin/doclens-all-in-one-entrypoint.sh

# 创建应用目录，并为归档解压、临时文件清理、脚本授权和运行用户设置权限。
# 1. 创建 APP_HOME，确保目标目录存在。
# 2. 去掉归档第一层目录后解压，保证文件直接落到 /opt/doclens 下。
# 3. 删除临时归档，避免把构建中间文件留在最终镜像中。
# 4. 给后端启动脚本和容器入口脚本增加可执行权限。
# 5. 将应用目录交给 postgres 用户，匹配一体化容器的运行用户边界。
RUN mkdir -p "${APP_HOME}" \
    && tar -xzf /tmp/doclens-server-dist.tar.gz --strip-components=1 -C "${APP_HOME}" \
    && rm -rf /tmp/doclens-server-dist.tar.gz \
    && chmod +x "${APP_HOME}/bin/doclens-server.sh" /usr/local/bin/doclens-all-in-one-entrypoint.sh \
    && chown -R postgres:postgres "${APP_HOME}"

# 声明后端 HTTP 服务端口；10003 提供 API 和后端托管的 /dashboard/ 前端入口。
# 声明 PostgreSQL 端口；5432 供部署工具和可选的宿主机端口映射识别。
EXPOSE 10003 5432

# 声明 PostgreSQL 数据、DocLens 文件存储和外部配置的挂载目录。
# 实际宿主机位置由 docker/x86/docker-compose.yml 的 bind mount 决定。
VOLUME ["/var/lib/postgresql/data", "/var/lib/doclens/storage", "/opt/doclens/config"]

# 指定容器启动入口；入口脚本会读取外挂配置、启动 PostgreSQL，再启动后端。
ENTRYPOINT ["doclens-all-in-one-entrypoint.sh"]
