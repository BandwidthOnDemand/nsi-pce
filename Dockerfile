FROM --platform=$BUILDPLATFORM maven:3.10-eclipse-temurin-17@sha256:a6d08f0b65a5420d67425b9851283b6b6f7f49b4b3cdfb234a4203bc59784124 AS MAVEN_BUILD

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
