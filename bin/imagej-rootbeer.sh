#!/bin/bash

# imagej-rootbeer.sh - script for Rootbeer-izing ImageJ

set -e

VERSION=1.47a
IJ=ij-$VERSION.jar
IJ_RB=ij-$VERSION-rootbeer.jar
IJ_RB_FIXED=ij-$VERSION-rootbeer-fixed.jar
URL=http://maven.imagej.net/content/repositories/releases/net/imagej/ij/$VERSION/$IJ

# download and build Rootbeer
echo
echo "========================= SETTING UP ROOTBEER =========================="
if [ ! -d rootbeer1 ];
then
  git clone git://github.com/pcpratts/rootbeer1.git
fi
cd rootbeer1
if [ ! -f Rootbeer.jar ];
then
  ant clean default
  ./pack-rootbeer
fi

# download ImageJ
echo
echo "========================= DOWNLOADING IMAGEJ ==========================="
if [ ! -f $IJ ];
then
  curl $URL > $IJ
fi

# Rootbeer-ize ImageJ
echo
echo "======================== ROOTBEER-IZING IMAGEJ ========================="
if [ ! -f $IJ_RB ];
then
  java -mx1g -jar Rootbeer.jar $IJ $IJ_RB
fi

# Transfer remaining resources from old ImageJ JAR file
echo
echo "=========================== FIXING JAR FILE ============================"
rm -rf t
mkdir t
cd t
jar xf ../$IJ
jar xf ../$IJ_RB
jar cmf META-INF/MANIFEST.MF ../$IJ_RB_FIXED .
cd ..
rm -rf t

# display results!
echo
echo "=============================== RESULTS ================================"
cd ..
ls -la rootbeer1/ij*.jar
