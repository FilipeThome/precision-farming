# Thin runtime fallback when a bootJar already exists under build/libs.
# Prefer the optimized multi-stage image: deploy/docker/Dockerfile.jvm (repo root context).
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl \
  && addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY build/libs/auth-service-*.jar app.jar
ENV SERVER_PORT=8080 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -fsS "http://127.0.0.1:${SERVER_PORT}/actuator/health" || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
