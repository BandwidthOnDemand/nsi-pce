FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-17@sha256:1a352420f7aba21f5ad08df31bab55f74c013fb491f1ae8ab1dd7ff9ed698584 AS MAVEN_BUILD

LABEL org.opencontainers.image.source=https://github.com/bandwidthondemand/nsi-pce

ARG VERSION
ENV HOME=/nsi-pce
WORKDIR $HOME
COPY . .
RUN mvn versions:set \
      -DnewVersion="${VERSION:?VERSION build argument is required}" -DgenerateBackupPoms=false \
    && mvn clean install -Dmaven.test.skip=true -Ddocker.nocache

# The Ubuntu-based JRE: the Alpine one is amd64 only, and the image is built for arm64 too.
FROM eclipse-temurin:17-jre@sha256:e04fa72be13b257a59fdec6dc14af78c8f80f035d3c1ed4e03e45552a231d4c8

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
