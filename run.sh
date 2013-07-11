#!/bin/bash
java -Xmx256m -Djava.net.preferIPv4Stack=true -jar target/nsi-pce-1.0-SNAPSHOT.one-jar.jar  $*
