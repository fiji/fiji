#!/bin/sh

find src-plugins modules -name \*.java |
grep -ve ij-plugins/Sun_JAI_Sample_IO_Source_Code \
	-e ij-plugins/Quickvol -e jython/sandbox \
	-e jython/installer/ \
	-e bio-formats/components/forks/jai/src/jj2000/j2k/ \
	-e envisaje/WinSDI/src/main/java/imagej/envisaje/winsdi/ |
./ImageJ "$@" \
	--javadoc --jarpath modules/bio-formats/jar \
	--jarpath modules/jython/jython/extlibs \
	--jarpath $HOME/.m2/repository/net/java/sezpoz/ \
	--jarpath /usr/share/java/ \
	-link http://download.java.net/media/java3d/javadoc/1.5.2/ \
	-link http://java.sun.com/j2se/1.5.0/docs/api/ \
	@/dev/stdin 2>&1
