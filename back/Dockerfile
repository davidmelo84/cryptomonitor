
FROM maven:3.9.9-eclipse-temurin-17-alpine AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B && \
    echo "✅ Build concluído: $(ls -lh target/*.jar)"


FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="ddevnordeste@gmail.com"
LABEL app="crypto-monitor"
LABEL version="2.0.1"

WORKDIR /app

RUN addgroup -S spring && \
    adduser -S spring -G spring && \
    chown -R spring:spring /app

COPY --from=build --chown=spring:spring /app/target/crypto-monitor.jar app.jar

USER spring:spring

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/crypto-monitor/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-Xmx512m", \
    "-Xms256m", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dserver.port=${PORT:-8080}", \
    "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}", \
    "-jar", "app.jar"]