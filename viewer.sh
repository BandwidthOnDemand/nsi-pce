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

KEYSTORE=$BASEDIR/config/keystore.jks
TRUSTSTORE=$BASEDIR/config/truststore.jks
PASSWORD="changeit"

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
        -Djavax.net.ssl.keyStore=$KEYSTORE \
        -Djavax.net.ssl.keyStorePassword=$PASSWORD \
        -Djavax.net.ssl.trustStore=$TRUSTSTORE \
        -Djavax.net.ssl.trustStorePassword=$PASSWORD \
	-cp target/pce.jar net.es.nsi.pce.visualization.TopologyViewer \
        "$@"

