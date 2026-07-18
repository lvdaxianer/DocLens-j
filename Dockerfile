ARG DOCLENS_RUNTIME_BASE_IMAGE=doclens:base-amd64

FROM ${DOCLENS_RUNTIME_BASE_IMAGE} AS runtime
USER root
ARG LOCAL_SERVER_DIST_ARCHIVE=docker/build/doclens-server-dist.tar.gz

ENV APP_HOME=/opt/doclens

COPY ${LOCAL_SERVER_DIST_ARCHIVE} /tmp/doclens-server-dist.tar.gz
COPY docker/entrypoint.sh /usr/local/bin/doclens-all-in-one-entrypoint.sh

RUN mkdir -p "${APP_HOME}" \
    && tar -xzf /tmp/doclens-server-dist.tar.gz --strip-components=1 -C "${APP_HOME}" \
    && rm -rf /tmp/doclens-server-dist.tar.gz \
    && chmod +x "${APP_HOME}/bin/doclens-server.sh" /usr/local/bin/doclens-all-in-one-entrypoint.sh \
    && chown -R postgres:postgres "${APP_HOME}"

EXPOSE 10003 5432
VOLUME ["/var/lib/postgresql/data", "/var/lib/doclens/storage", "/opt/doclens/config"]

ENTRYPOINT ["doclens-all-in-one-entrypoint.sh"]
