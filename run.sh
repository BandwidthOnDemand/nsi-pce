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

java -Xmx256m -Djava.net.preferIPv4Stack=true  \
	-Dapp.home="$BASEDIR" \
  	-Dbasedir="$BASEDIR" \
	-Djava.util.logging.config.file="$BASEDIR/config/logging.properties" \
	-Dcom.sun.xml.bind.v2.runtime.JAXBContextImpl.fastBoot=true \
	-jar target/nsi-pce-1.0-SNAPSHOT.one-jar.jar  $*
