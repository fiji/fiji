#!/bin/sh

die () {
	echo "$*" >&2
	exit 1
}

PYTHON_LIB=
for version in 2.6 2.5
do
	test -d /usr/lib/python$version && PYTHON_LIB=/usr/lib/python$version
done

../fiji --ant -Dpython.lib=$PYTHON_LIB -f jython/build.xml jar-complete copy-lib >&2 ||
die "Could not run ant"

cd jython/dist &&
zip -d jython.jar com/sun/jna/\* &&
cp jython.jar ../../ &&
zip -9r ../../jython.jar Lib ||
die "Could not add Lib/ to jython.jar"
