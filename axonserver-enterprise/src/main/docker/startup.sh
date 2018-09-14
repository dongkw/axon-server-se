#!/bin/bash

DOMAIN=`hostname -d`
if [[ "$DOMAIN" = "" ]]; then
  echo "No domain"
else
  echo >> axonhub.properties
  echo "axoniq.axonserver.domain=$DOMAIN" >> axonserver.properties
fi

/register.sh &
java -Djava.security.egd=file:/dev/./urandom -Xmx512m -jar app.jar
