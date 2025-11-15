FROM eclipse-temurin:21
RUN mkdir /opt/app
RUN mkdir -p /mn-data
COPY mn-api.jar /opt/app/mn-api.jar
WORKDIR /mn_data
CMD ["java", "-jar", "/opt/app/mn-api.jar"]

