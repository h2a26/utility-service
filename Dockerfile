# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom and fetch dependencies (improves build speed via caching)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source and build the JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create the temp directory for Excel processing
RUN mkdir -p /app/batch-temp && chmod 777 /app/batch-temp

# Copy the built JAR from the build stage
COPY --from=build /app/target/utility-service.jar app.jar

# Optimize JVM for container environments
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]