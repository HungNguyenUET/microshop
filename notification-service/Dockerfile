# syntax=docker/dockerfile:1
# Image doc lap cua notification-service: build tu source, khong can gi ngoai thu muc nay.
#   docker build -t microshop/notification-service .

# ---- build stage: Maven + JDK 25 (cache ~/.m2 giua cac lan build nho BuildKit)
FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /src
COPY pom.xml .
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

# ---- runtime stage: chi JRE + app.jar
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --chmod=755 healthcheck.sh /usr/local/bin/healthcheck
COPY --from=build /src/target/app.jar app.jar

# Heap theo % RAM cua container; SerialGC nhe RAM khi chay nhieu JVM tren 1 laptop
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1"

EXPOSE 8084
HEALTHCHECK --interval=5s --timeout=3s --start-period=20s --retries=40 CMD ["healthcheck", "8084"]

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
