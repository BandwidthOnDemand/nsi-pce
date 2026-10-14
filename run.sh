#!/bin/bash

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

PRGDIR=`dirname "$PRG"`
BASEDIR=`cd "$PRGDIR" >/dev/null; pwd`

# Outbound TLS from PEM files, each optional: TLS_CERT and TLS_KEY (PKCS#8) for the client
# certificate, TLS_CA for the CA bundle that replaces the JVM's default trust.
TLS_OPTS=()
[ -n "$TLS_CERT" ] && TLS_OPTS+=("-Dnsi.tls.certificate=$TLS_CERT")
[ -n "$TLS_KEY" ] && TLS_OPTS+=("-Dnsi.tls.privateKey=$TLS_KEY")
[ -n "$TLS_CA" ] && TLS_OPTS+=("-Dnsi.tls.trustedCertificates=$TLS_CA")

java -Xmx2048m -Djava.net.preferIPv4Stack=true  \
	-Dapp.home="$BASEDIR" \
	-Dbasedir="$BASEDIR" \
	-Djava.util.logging.config.file="$BASEDIR/config/logging.properties" \
	-Dorg.glassfish.jaxb.runtime.v2.runtime.JAXBContextImpl.fastBoot=true \
	"${TLS_OPTS[@]}" \
	-jar target/pce.jar \
	-topologyConfigFile config/topology-dds.xml \
	$*

