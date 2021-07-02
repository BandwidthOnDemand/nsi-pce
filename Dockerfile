FROM maven:3-openjdk-8 AS MAVEN_BUILD

ENV HOME /nsi-pce
WORKDIR $HOME
COPY . .
RUN mvn clean install -Dmaven.test.skip=true -Ddocker.nocache
 
FROM openjdk:8-jre-alpine3.9

ENV HOME /nsi-pce
USER 1000:1000
WORKDIR $HOME
COPY --from=MAVEN_BUILD $HOME/target/pce.jar .
COPY --from=MAVEN_BUILD $HOME/config ./config

EXPOSE 8080/tcp
CMD java \
    -Dbasedir=/nsi-pce \
    -Dapp.home=/nsi-pce \
    -Xmx1024m -Djava.net.preferIPv4Stack=true  \
    -Dcom.sun.xml.bind.v2.runtime.JAXBContextImpl.fastBoot=true \
    -Djava.util.logging.config.file=/nsi-pce/config/logging.properties \
    -jar /nsi-pce/pce.jar
