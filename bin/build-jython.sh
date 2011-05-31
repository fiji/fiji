#!/bin/sh

die () {
	echo "$*" >&2
	exit 1
}

PYTHON_LIB=/usr/lib/python2.5
if test ! -f "$PYTHON_LIB"/re.py
then
	PYTHON_LIB="$(pwd)/python-d5876b1"
	if test ! -d "$PYTHON_LIB"
	then
		curl "http://fiji.sc/cgi-bin/gitweb.cgi?p=python/.git;a=snapshot;h=d5876b11b8c086b51b73ec5f32a309b425be906a;sf=tgz" | tar xzvf -
	fi
fi

../../fiji --ant -Dpython.lib="$PYTHON_LIB" -f jython/build.xml jar-complete copy-lib >&2 ||
die "Could not run ant"

cd jython/dist &&
(zip -d jython.jar com/sun/jna/\* || true) &&
cp jython.jar ../../ &&
zip -9r ../../jython.jar Lib ||
die "Could not add Lib/ to jython.jar"
