#!/bin/sh

CWD="$(cd "$(dirname "$0")" && pwd)"
BASEURL=http://ftp.fernuni-hagen.de/ftp-dir/pub/mirrors/www.apache.org/maven/binaries/
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
	 curl $BASEURL/$TAR > $TAR &&
	 tar xzvf $TAR &&
	 rm $TAR) ||
	die "Could not get maven"
fi

FIJI="$CWD/../fiji"
JAVA_HOME="$("$FIJI" --print-java-home)/.."
JAVACMD="$JAVA_HOME/bin/java"
export JAVACMD JAVA_HOME

exec "$MVN" "$@"
