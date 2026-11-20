# ==========================================
# Stage 1: Build the Spring Boot application
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper and pom.xml first to cache dependency downloads
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code and build production jar
COPY src/ ./src/
RUN ./mvnw clean package -DskipTests

# ==========================================
# Stage 2: Minimal Production JRE Runtime
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root application user for container security
RUN addgroup -S devprofile && adduser -S devprofile -G devprofile
USER devprofile:devprofile

# Copy executable jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# Configure JVM container memory awareness and run
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
