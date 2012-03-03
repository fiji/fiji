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
		curl --help > /dev/null 2>&1
		if test $? = 127
		then
			curl () {
				wget -O /dev/stdout "$1"
			}
		fi
		curl "http://fiji.sc/cgi-bin/gitweb.cgi?p=python/.git;a=snapshot;h=d5876b11b8c086b51b73ec5f32a309b425be906a;sf=tgz" | tar xzvf -
	fi ||
	die "Could not fetch the Python library files"
fi

../../ImageJ --ant -Dpython.lib="$PYTHON_LIB" -f jython/build.xml jar-complete copy-lib >&2 ||
die "Could not run ant"

case $(uname -s) in
    Darwin)
        JAR=jar
        ;;
    *)
        JAR=$(../../ImageJ --print-java-home)/../bin/jar
        ;;
esac

rm -rf unpacked &&
mkdir unpacked && (
	cd unpacked &&
	"$JAR" xf ../jython/dist/jython.jar &&
	rm -rf com/sun/jna &&
	cp -R ../jython/dist/Lib ./ &&
        "$JAR" cf ../jython.jar *
) ||
die "Could not add Lib/ to jython.jar"
