FROM eclipse-temurin:21-jdk

# gradle distribution is embedded in the image, so the project gradle wrapper
# (gradle/wrapper/gradle-wrapper.jar) is not needed to build
# keep GRADLE_VERSION in sync with gradle/wrapper/gradle-wrapper.properties
ARG GRADLE_VERSION=9.4.1

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl unzip \
    && rm -rf /var/lib/apt/lists/*

RUN curl -fsSL -o /tmp/gradle.zip \
      "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
    && unzip -q /tmp/gradle.zip -d /opt \
    && mv "/opt/gradle-${GRADLE_VERSION}" /opt/gradle \
    && rm /tmp/gradle.zip

ENV PATH="/opt/gradle/bin:${PATH}"

# project root is mounted on /app (build.gradle, src, docker/jar)
# gradle home is kept inside the mount so caches survive between runs
# and stay writable when the container runs as the host user
ENV GRADLE_USER_HOME=/app/.gradle-docker

WORKDIR /app

RUN echo "cd /app" > /build.sh \
    && echo "[ -f ./build.gradle ] || { echo 'ERROR: mount the project root on /app'; exit 1; }" >> /build.sh \
    && echo "gradle --no-daemon dockerJar" >> /build.sh

CMD ["/bin/sh", "/build.sh"]
