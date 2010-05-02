#!/bin/sh

die () {
	echo "$*" >&2
	exit 1
}

../fiji --ant -Dpython.lib=/usr/lib/python2.5 -f jython/build.xml jar-complete copy-lib >&2 ||
die "Could not run ant"

cd jython/dist &&
cp jython.jar ../../ &&
zip -9r ../../jython.jar Lib ||
die "Could not add Lib/ to jython.jar"
