# syntax=docker/dockerfile:1.7

ARG NODE_IMAGE=node:22-bookworm-slim
ARG MAVEN_IMAGE=maven:3.9.9-eclipse-temurin-21
ARG POSTGRES_IMAGE=postgres:16-bookworm

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
RUN mvn -pl doclens-server -am -Pdist -DskipTests package

FROM ${POSTGRES_IMAGE} AS runtime
USER root

ARG LOCAL_JDK_ARCHIVE=docker/jdk/temurin-21-jdk-linux-x64.tar.gz

ENV APP_HOME=/opt/doclens \
    JAVA_HOME=/opt/java/openjdk \
    PATH=/opt/java/openjdk/bin:${PATH}

COPY ${LOCAL_JDK_ARCHIVE} /tmp/local-jdk.tar.gz
COPY --from=server-build /workspace/doclens-server/target/doclens-server-*-dist.tar.gz /tmp/doclens-server-dist.tar.gz
COPY docker/entrypoint.sh /usr/local/bin/doclens-all-in-one-entrypoint.sh

RUN mkdir -p "${APP_HOME}" "${JAVA_HOME}" /var/lib/doclens/storage \
    && tar -xzf /tmp/local-jdk.tar.gz --strip-components=1 -C "${JAVA_HOME}" \
    && tar -xzf /tmp/doclens-server-dist.tar.gz --strip-components=1 -C "${APP_HOME}" \
    && rm /tmp/local-jdk.tar.gz /tmp/doclens-server-dist.tar.gz \
    && chmod +x "${APP_HOME}/bin/doclens-server.sh" /usr/local/bin/doclens-all-in-one-entrypoint.sh \
    && chown -R postgres:postgres /var/lib/doclens /var/lib/postgresql /var/run/postgresql

EXPOSE 10003 5432
VOLUME ["/var/lib/postgresql/data", "/var/lib/doclens/storage"]

ENTRYPOINT ["doclens-all-in-one-entrypoint.sh"]
