# Build stage
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Copy gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle* ./
COPY src ./src

# Make gradlew executable and build
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar
COPY --from=builder /app/build/libs/*.jar app.jar

# JVM options for performance
# Paper reference (Section 3.4):
# "Critical JVM settings: 80% heap, G1GC, 100ms GC target"
ENV JAVA_OPTS="-XX:+UseG1GC \
    -XX:MaxGCPauseMillis=100 \
    -XX:+ParallelRefProcEnabled \
    -XX:MaxRAMPercentage=80.0 \
    -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
