#!/bin/sh

CLASSPATH=
add_to_classpath () {
	if test -d "$1"
	then
		for file in "$1"/*
		do
			add_to_classpath "$file"
		done
	else
		case "$1" in
		*.jar)
			CLASSPATH="$CLASSPATH${CLASSPATH+:}$1"
			;;
		esac
	fi
}

add_to_classpath modules/bio-formats/jar
add_to_classpath modules/jython/jython/extlibs
add_to_classpath $HOME/.m2/repository/net/java/sezpoz/
add_to_classpath /usr/share/java/
add_to_classpath modules/ij-plugins/
add_to_classpath $HOME/.m2/repository/com/apple/AppleJavaExtensions/1.5/

find src-plugins modules -name \*.java |
grep -ve src-plugins/FFMPEG_IO/fiji/ffmpeg/ \
	-e ij-plugins/Sun_JAI_Sample_IO_Source_Code \
	-e ij-plugins/Quickvol \
	-e modules/ij-plugins/VTK-Examples/ \
	-e modules/ij-plugins/itk4j/ \
	-e modules/ij-plugins/ij-VTK/ \
	-e modules/commons-math/src/site/ \
	-e jython/sandbox -e jython/installer/ \
	-e modules/jython/jython/src/com/ziclix/python/sql/handler/OracleDataHandler.java \
	-e modules/jython/jython/src/com/ziclix/python/sql/handler/InformixDataHandler.java \
	-e modules/jython/jython/Demo/jreload/ \
	-e modules/batik/sources/org/apache/batik/ext/awt/image/codec/tiff/ \
	-e modules/batik/sources/org/apache/batik/ext/awt/image/codec/jpeg/ \
	-e bio-formats/components/forks/jai/src/jj2000/j2k/ \
	-e modules/bio-formats/components/bio-formats/utils/mipav/PlugInBioFormatsImporter.java \
	-e modules/imglib/imglib./ij/src/test/java/tests/PerformanceBenchmark.java \
	-e modules/imglib/imglib2/broken/ \
	-e modules/imagej2/ui/pivot/ \
	-e modules/imagej2/ui/swt/ \
	-e modules/imagej2/extra/ \
	-e modules/imagej2/opencl/ \
	-e envisaje/ \
	-e modules/weka/wekaexamples/ |
javadoc -classpath "$CLASSPATH" "$@" \
	-link http://download.java.net/media/java3d/javadoc/1.5.2/ \
	-link http://java.sun.com/j2se/1.5.0/docs/api/ \
	@/dev/stdin 2>&1
