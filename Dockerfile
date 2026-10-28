FROM --platform=$BUILDPLATFORM maven:3.10-eclipse-temurin-17@sha256:b67db904974d04b9e6eba5b073ae227569ee1cae0d1f26be8993525291ef76dc AS MAVEN_BUILD

LABEL org.opencontainers.image.source=https://github.com/bandwidthondemand/nsi-pce

ARG VERSION
ENV HOME=/nsi-pce
WORKDIR $HOME
COPY . .
RUN mvn versions:set \
      -DnewVersion="${VERSION:?VERSION build argument is required}" -DgenerateBackupPoms=false \
    && mvn clean install -Dmaven.test.skip=true -Ddocker.nocache

# The Ubuntu-based JRE: the Alpine one is amd64 only, and the image is built for arm64 too.
FROM eclipse-temurin:17-jre@sha256:4faa9eaf440514dd4f7ef71fecb92526e13679aea039bed5d2fc98e885c60dee

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
