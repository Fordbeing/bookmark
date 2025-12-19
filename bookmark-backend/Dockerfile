# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Use Aliyun Maven mirror for better network stability in China
RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?>\
    <settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" \
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" \
    xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">\
    <mirrors>\
    <mirror>\
    <id>aliyun</id>\
    <mirrorOf>central</mirrorOf>\
    <name>Aliyun Maven</name>\
    <url>https://maven.aliyun.com/repository/central</url>\
    </mirror>\
    </mirrors>\
    </settings>' > /root/.m2/settings.xml

COPY pom.xml .
# Download dependencies first (for better caching)
RUN mvn dependency:go-offline -B || mvn dependency:go-offline -B || mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
