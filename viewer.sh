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

# Outbound TLS from PEM files, as in run.sh: TLS_CERT, TLS_KEY (PKCS#8) and TLS_CA, each optional.
TLS_OPTS=()
[ -n "$TLS_CERT" ] && TLS_OPTS+=("-Dnsi.tls.certificate=$TLS_CERT")
[ -n "$TLS_KEY" ] && TLS_OPTS+=("-Dnsi.tls.privateKey=$TLS_KEY")
[ -n "$TLS_CA" ] && TLS_OPTS+=("-Dnsi.tls.trustedCertificates=$TLS_CA")

if [ ! -f "$BASEDIR/target/pce.jar" ]; then
    echo "$BASEDIR/target/pce.jar not found; build it first with ./mvnw -DskipTests package" >&2
    exit 1
fi

# The viewer reads config/ relative to the working directory.
cd "$BASEDIR"

java -Xmx1536m -Djava.net.preferIPv4Stack=true  \
        -Dapp.home="$BASEDIR" \
        -Dbasedir="$BASEDIR" \
        -Djava.util.logging.config.file="$BASEDIR/config/logging.properties" \
        -Dorg.glassfish.jaxb.runtime.v2.runtime.JAXBContextImpl.fastBoot=true \
        "${TLS_OPTS[@]}" \
	-cp target/pce.jar net.es.nsi.pce.visualization.TopologyViewer \
        "$@"

