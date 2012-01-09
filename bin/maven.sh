#!/bin/sh

CWD="$(cd "$(dirname "$0")" && pwd)"
BASEURL=http://www.apache.org/dyn/closer.cgi/maven/binaries/
DIR=apache-maven-2.2.1
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

if test Darwin = "$(uname -s)"
then
	JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/
	JAVACMD=$JAVA_HOME/Commands/java
else
	FIJI="$CWD/../ImageJ"
	JAVA_HOME="$("$FIJI" --print-java-home)/.."
	JAVACMD="$JAVA_HOME/bin/java"
fi
export JAVACMD JAVA_HOME

exec "$MVN" "$@"
