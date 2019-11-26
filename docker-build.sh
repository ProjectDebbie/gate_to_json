#!/bin/sh

BASEDIR=/usr/local
GATE_EXPORT_JSON_HOME="${BASEDIR}/share/gatetojson/"

GATE_EXPORT_JSON_VERSION=1.0

# Exit on error
set -e
 
if [ $# -ge 1 ] ; then
	ADES_EXPORT_JSON_VERSION="$1"
fi

if [ -f /etc/alpine-release ] ; then
	# Installing OpenJDK 8
	apk add --update openjdk8-jre
	
	# ades development dependencies 
	apk add openjdk8 git maven
else
	# Runtime dependencies
	apt-get update
	apt-get install openjdk-8-jre
	
	# The development dependencies
	apt-get install openjdk-8-jdk git maven
fi

mvn clean install -DskipTests

#rename jar
mv target/gate_to_json-0.0.1-SNAPSHOT-jar-with-dependencies.jar gate_to_json-${GATE_EXPORT_JSON_VERSION}.jar

cat > /usr/local/bin/gate_to_json <<EOF
#!/bin/sh
exec java \$JAVA_OPTS -jar "${GATE_EXPORT_JSON_HOME}/gate_to_json-${GATE_EXPORT_JSON_VERSION}.jar" -workdir "${GATE_EXPORT_JSON_HOME}" "\$@"
EOF
chmod +x /usr/local/bin/gate_to_json

#delete target
rm -R target src pom.xml

#add bash for nextflow
apk add bash

if [ -f /etc/alpine-release ] ; then
	# Removing not needed tools
	apk del openjdk8 git maven
	rm -rf /var/cache/apk/*
else
	apt-get remove openjdk-8-jdk git maven
	rm -rf /var/cache/dpkg
fi

