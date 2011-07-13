#!/bin/sh

PWD=`pwd`
FIJIF="$PWD/../../"
JARF="$FIJIF/jars/"
VIBF="$FIJIF/src-plugins/VIB-lib/"
JAVAF="$FIJIF/java/linux/jdk1.6.0_21/"
JAVAC="$JAVAF/bin/javac"
JAR="$JAVAF/bin/jar"

CLASSPATH=".:$JARF/ij.jar:$JARF/imglib.jar:$JARF/Jama-1.0.2.jar"

JAVACOPTS="-source 1.5 -target 1.5 -classpath $CLASSPATH"
IJ3D_SRC="`find . -type f` `find . -name \*.png` plugins.config"
VIB_SRC="
	math3d/*.java \
	nrrd/*.java \
	vib/NaiveResampler.java \
	vib/InterpolatedImage.java \
	vib/PointList.java \
	vib/BenesNamedPoint.java \
	vib/FastMatrix.java \
	vib/FloatMatrix.java \
	vib/segment/ImageButton.java \
	vib/segment/Border.java \
	amira/AmiraParameters.java \
	amira/AmiraTable.java"

test ! -d tempdir || rm -rf tempdir
mkdir tempdir
tar cvf - $IJ3D_SRC | (cd tempdir; tar xvf -)
(cd $VIBF && tar cvf - $VIB_SRC) | (cd tempdir; tar xvf -)


(cd tempdir && \
	rm -rf ImageJ_3D_Viewer.jar && \
	$JAVAC $JAVACOPTS ImageJ_3D_Viewer.java && \
	$JAR cvf ../ImageJ_3D_Viewer.jar `find . -type f`) && \
rm -rf tempdir

