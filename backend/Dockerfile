FROM openjdk:17.0.1-jdk-slim

WORKDIR /app

COPY ./build/libs/backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.config.location=/config/application.yml"]