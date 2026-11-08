# Dung chung cho moi Spring Boot service. Jar build san bang: ./mvnw clean package -DskipTests
FROM eclipse-temurin:25-jre

ARG MODULE
WORKDIR /app

COPY docker/healthcheck.sh /usr/local/bin/healthcheck
COPY ${MODULE}/target/app.jar app.jar

# Heap theo % RAM cua container; SerialGC nhe RAM khi chay nhieu JVM tren 1 laptop
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
