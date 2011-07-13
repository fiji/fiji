#!/bin/sh

PWD=`pwd`
FIJIF="$PWD/../../"
JARF="$FIJIF/jars/"
VIBF="$FIJIF/src-plugins/VIB-lib/"
JAVAF="$FIJIF/java/linux/jdk1.6.0_21"

JAVADOC="$JAVAF/bin/javadoc"

CLASSPATH=".:$JARF/junit-4.5.jar:$JARF/jzlib-1.0.7.jar:$JARF/ij.jar"

if [ ! -f ImageJ_3D_Viewer.jar ]; then
	echo "Please create an up-to-date ImageJ_3D_Viewer.jar first"
	exit;
fi

test ! -d tmp || rm -rf tmp;
test ! -d docs || rm -rf docs;

mkdir tmp docs;
cp ImageJ_3D_Viewer.jar tmp/
(cd tmp &&
    jar -xf ImageJ_3D_Viewer.jar &&
    test ! -d META-INF || rm -rf META-INF &&
    test ! -f plugins.config || rm -rf plugins.config &&
    $JAVADOC -classpath $CLASSPATH -d ../docs/ *)

rm -rf doc

