ARG NODE_IMAGE=node:22-bookworm-slim
ARG MAVEN_IMAGE=maven:3.9.9-eclipse-temurin-21
ARG UBUNTU_IMAGE=ubuntu:22.04

FROM ${NODE_IMAGE} AS dashboard-build
WORKDIR /workspace
ENV NODE_OPTIONS=--max-old-space-size=768
COPY doclens-dashboard/package.json doclens-dashboard/package-lock.json ./doclens-dashboard/
WORKDIR /workspace/doclens-dashboard
RUN npm ci
COPY doclens-dashboard/ ./
RUN mkdir -p /workspace/doclens-server/src/main/resources/static && npm run build

FROM ${MAVEN_IMAGE} AS server-build
WORKDIR /workspace
COPY . .
COPY --from=dashboard-build /workspace/doclens-server/src/main/resources/static/dashboard ./doclens-server/src/main/resources/static/dashboard
RUN mvn -pl doclens-server -am -Pdist -Dmaven.test.skip=true package

FROM ${UBUNTU_IMAGE} AS runtime
USER root

ENV DEBIAN_FRONTEND=noninteractive \
    TZ=Etc/UTC

ARG LOCAL_JDK_ARCHIVE=docker/runtime/jdk-21_linux-x64_bin.tar.gz
ARG LOCAL_NODE_ARCHIVE=docker/runtime/node-v22-linux-x64.tar.gz
ARG LOCAL_POSTGRES_DEB_ARCHIVE=docker/runtime/postgresql-16-ubuntu22.04-x64-debs.tar.gz

ENV APP_HOME=/opt/doclens \
    JAVA_HOME=/opt/java/openjdk \
    NODE_HOME=/opt/nodejs \
    PG_MAJOR=16 \
    PGDATA=/var/lib/postgresql/data \
    PATH=/opt/java/openjdk/bin:/opt/nodejs/bin:/usr/lib/postgresql/16/bin:${PATH}

COPY ${LOCAL_JDK_ARCHIVE} /tmp/local-jdk.tar.gz
COPY ${LOCAL_NODE_ARCHIVE} /tmp/local-node.tar.gz
COPY ${LOCAL_POSTGRES_DEB_ARCHIVE} /tmp/local-postgres-debs.tar.gz
COPY --from=server-build /workspace/doclens-server/target/doclens-server-*-dist.tar.gz /tmp/doclens-server-dist.tar.gz
COPY docker/entrypoint.sh /usr/local/bin/doclens-all-in-one-entrypoint.sh

RUN mkdir -p "${APP_HOME}" "${JAVA_HOME}" "${NODE_HOME}" /tmp/postgres-debs /var/lib/postgresql/data /var/lib/doclens/storage /var/run/postgresql \
    && tar -xzf /tmp/local-jdk.tar.gz --strip-components=1 -C "${JAVA_HOME}" \
    && tar -xzf /tmp/local-node.tar.gz --strip-components=1 -C "${NODE_HOME}" \
    && tar -xzf /tmp/local-postgres-debs.tar.gz -C /tmp/postgres-debs \
    && DEBIAN_FRONTEND=noninteractive TZ=Etc/UTC dpkg -i /tmp/postgres-debs/*.deb \
    && tar -xzf /tmp/doclens-server-dist.tar.gz --strip-components=1 -C "${APP_HOME}" \
    && rm -rf /tmp/local-jdk.tar.gz /tmp/local-node.tar.gz /tmp/local-postgres-debs.tar.gz /tmp/postgres-debs /tmp/doclens-server-dist.tar.gz \
    && chmod +x "${APP_HOME}/bin/doclens-server.sh" /usr/local/bin/doclens-all-in-one-entrypoint.sh \
    && (getent group postgres >/dev/null || groupadd -r postgres) \
    && (id -u postgres >/dev/null 2>&1 || useradd -r -g postgres -d /var/lib/postgresql -s /bin/bash postgres) \
    && chown -R postgres:postgres /var/lib/doclens /var/lib/postgresql /var/run/postgresql

EXPOSE 10003 5432
VOLUME ["/var/lib/postgresql/data", "/var/lib/doclens/storage"]

ENTRYPOINT ["doclens-all-in-one-entrypoint.sh"]
