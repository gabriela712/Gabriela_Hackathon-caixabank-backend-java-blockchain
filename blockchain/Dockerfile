# Etapa de construcción
FROM maven:3.9.9-eclipse-temurin-21 as build

WORKDIR /app

COPY pom.xml ./
COPY src ./src

RUN mvn clean package -DskipTests

# Etapa de ejecución
FROM openjdk:21-jdk-slim

WORKDIR /app

COPY --from=build /app/target/blockchain-0.0.1-SNAPSHOT.jar /app/blockchain-0.0.1-SNAPSHOT.jar

EXPOSE 3000

ENTRYPOINT ["java", "-jar", "/app/blockchain-0.0.1-SNAPSHOT.jar"]