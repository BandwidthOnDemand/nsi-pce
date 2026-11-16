FROM --platform=$BUILDPLATFORM maven:3.10-eclipse-temurin-17@sha256:0362bf77ae7b21e3e0a0f4b211e85eb973e2cf9a680b2d36210e580336bb2255 AS MAVEN_BUILD

LABEL org.opencontainers.image.source=https://github.com/bandwidthondemand/nsi-pce

ARG VERSION
ENV HOME=/nsi-pce
WORKDIR $HOME
COPY . .
RUN mvn versions:set \
      -DnewVersion="${VERSION:?VERSION build argument is required}" -DgenerateBackupPoms=false \
    && mvn clean install -Dmaven.test.skip=true -Ddocker.nocache

# The Ubuntu-based JRE: the Alpine one is amd64 only, and the image is built for arm64 too.
FROM eclipse-temurin:17-jre@sha256:fb2cec8da84e3f086b59e29bf432d372e396fe0c247adcfb89303a28c2e313c0

ENV HOME=/nsi-pce
USER 1000:1000
WORKDIR $HOME
COPY --from=MAVEN_BUILD $HOME/target/pce.jar .
COPY --from=MAVEN_BUILD $HOME/config ./config

EXPOSE 8080/tcp
CMD ["java", \
     "-Dbasedir=/nsi-pce", \
     "-Dapp.home=/nsi-pce", \
     "-Xmx1024m", "-Djava.net.preferIPv4Stack=true", \
     "-Dorg.glassfish.jaxb.runtime.v2.runtime.JAXBContextImpl.fastBoot=true", \
     "-Djava.util.logging.config.file=/nsi-pce/config/logging.properties", \
     "-jar", "/nsi-pce/pce.jar"]
