#!/bin/sh

PWD=`pwd`
FIJIF="$PWD/../../"
JARF="$FIJIF/jars/"
VIBF="$FIJIF/src-plugins/VIB-lib/src/main/java"
JAVAC="/usr/bin/javac"
JAR="/usr/bin/jar"
J3DF="$FIJIF/java/macosx-java3d/Home/lib/ext/"

IJ=`find $JARF -name ij-?.???.jar`
JAMA=`find $JARF -name Jama-*.jar`
IMGLIB=`find $JARF -name imglib-2.0.0-SNAPSHOT.jar`

CLASSPATH=".:$IJ:$JAMA:$IMGLIB:$J3DF/j3dcore.jar:$J3DF/j3dutils.jar:$J3DF/vecmath.jar"

JAVACOPTS="-source 1.5 -target 1.5 -extdirs $J3DF -classpath $CLASSPATH"
IJ3D_SRC=`(cd src/main/java && find . -type f)`
IJ3D_RSRC=`(cd src/main/resources && find . -type f)`

VIB_SRC="
	math3d/*.java \
	nrrd/*.java \
	process3d/Smooth.java \
	process3d/Convolve3d.java \
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
(cd src/main/resources && tar cvf - $IJ3D_RSRC) | (cd tempdir; tar xvf -)
(cd src/main/java && tar cvf - $IJ3D_SRC) | (cd tempdir; tar xvf -)
(cd $VIBF && tar cvf - $VIB_SRC) | (cd tempdir; tar xvf -)


(cd tempdir && \
	rm -rf ImageJ_3D_Viewer.jar && \
	$JAVAC $JAVACOPTS ImageJ_3D_Viewer.java isosurface/Show_Colour_Surfaces.java process3d/Smooth_.java && \
	$JAR cvf ../ImageJ_3D_Viewer.jar `find . -type f`) && \
rm -rf tempdir

