# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build

ARG TARGETARCH
WORKDIR /workspace

COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn --batch-mode --no-transfer-progress clean package -DskipTests

# sqlite-jdbc cannot discover a native library from a second nested JAR in every
# Spring Boot launcher/runtime combination. Extract exactly one Linux library and
# give its path to the JVM explicitly in the final image.
RUN --mount=type=cache,target=/root/.m2 \
    case "${TARGETARCH}" in \
        amd64) sqlite_arch=x86_64 ;; \
        arm64) sqlite_arch=aarch64 ;; \
        *) echo "Unsupported container architecture: ${TARGETARCH}" >&2; exit 1 ;; \
    esac \
    && mkdir -p /sqlite-native \
    && cd /tmp \
    && jar xf "/root/.m2/repository/org/xerial/sqlite-jdbc/3.53.4.0/sqlite-jdbc-3.53.4.0-natives-all.jar" \
        "org/sqlite/native/Linux/${sqlite_arch}/libsqlitejdbc.so" \
    && cp "org/sqlite/native/Linux/${sqlite_arch}/libsqlitejdbc.so" /sqlite-native/libsqlitejdbc.so

FROM eclipse-temurin:21-jre

RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app app \
    && mkdir -p /app /data \
    && chown -R app:app /app /data

WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/secure-rest-api-*.jar app.jar
COPY --from=build --chown=app:app /sqlite-native/libsqlitejdbc.so /app/native/libsqlitejdbc.so

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-Dorg.sqlite.lib.path=/app/native", "-jar", "/app/app.jar"]
