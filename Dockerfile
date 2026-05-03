FROM eclipse-temurin:21-jre-jammy
RUN groupadd --system app && useradd --system --gid app --home /app app \
 && apt-get update && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
USER app

# Expects `mvn -DskipTests package` to have produced target/oligarch-rating.jar.
# CI builds the jar in a separate step; this image just packages and runs it.
COPY --chown=app:app target/oligarch-rating.jar /app/app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=3 --start-period=30s \
  CMD curl -fsS http://localhost:8080/actuator/health/liveness || exit 1

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar \"$@\"", "--"]
