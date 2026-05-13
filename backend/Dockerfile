# -------- Stage 1: Build with Maven --------
FROM maven:3.9.9-amazoncorretto-21 AS build
WORKDIR /app

# Copy pom.xml first (cached layer — only re-runs if pom.xml changes)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# -------- Stage 2: Production runtime --------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Fix OKD permissions — allow random UID to run
RUN chgrp -R 0 /app && chmod -R g+rwx /app

# Copy compiled JAR from build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]