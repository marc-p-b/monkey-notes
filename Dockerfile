FROM openjdk:17-jdk-alpine
COPY dsb-0.0.1-SNAPSHOT.jar /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]