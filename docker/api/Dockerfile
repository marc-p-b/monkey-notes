FROM eclipse-temurin:21
RUN mkdir /opt/app
RUN mkdir -p /api-data
COPY api.jar /opt/app/api.jar
WORKDIR /api-data
CMD ["java", "-jar", "/opt/app/api.jar"]

