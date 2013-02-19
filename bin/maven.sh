#!/bin/sh

CWD="$(cd "$(dirname "$0")" && pwd)"
VERSION=3.0.4
BASEURL=http://www.apache.org/dyn/closer.cgi/maven/maven-3/$VERSION/binaries/
DIR=apache-maven-$VERSION
TAR=$DIR-bin.tar.gz
MVN="$CWD/$DIR/bin/mvn"

die () {
	echo "$*" >&2
	exit 1
}

if ! test -x "$MVN"
then
	(cd "$CWD" &&
	 mirrorURL="$(curl $BASEURL/$TAR |
		sed -ne 's/^.*<a href="\([^"]*'"$TAR"'\)".*/\1/p' |
		sed -e '1q')" &&
	 curl "$mirrorURL" > $TAR &&
	 tar xzvf $TAR &&
	 rm $TAR) ||
	die "Could not get maven"
fi

if test ! -d "$JAVA_HOME"
then
	if test Darwin = "$(uname -s)"
	then
		JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/
		export JAVA_HOME
		JAVACMD=$JAVA_HOME/Commands/java
		export JAVACMD
	else
		FIJI="$CWD/../ImageJ"
		FIJI_JAVA_HOME="$("$FIJI" --print-java-home)"
		if test -d "$FIJI_JAVA_HOME"
		then
			JAVA_HOME="${FIJI_JAVA_HOME%/jre/}"
			JAVA_HOME="${JAVA_HOME%/jre}"
			export JAVA_HOME
			JAVACMD="$JAVA_HOME/bin/java"
			export JAVACMD
		fi
	fi
fi

exec "$MVN" "$@"
