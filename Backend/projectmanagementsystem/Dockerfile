FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn clean install -DskipTests
COPY src ./src
RUN mvn package -DskipTests

FROM openjdk:17-jdk-slim

COPY --from=build /app/target/projectmanagementsystem-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]

