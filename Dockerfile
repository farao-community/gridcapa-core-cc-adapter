FROM openjdk:11.0-jre-slim

ARG JAR_FILE=core-valid-adapter-app/target/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
