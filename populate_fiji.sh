#!/bin/sh

#
# populate_fiji.sh
#
# This script generates a local Fiji.app.

# -- Functions --

die() {
  echo "$@" 1>&2
  exit 1
}

# -- Determine correct ImageJ launcher executable --

case "$(uname -s),$(uname -m)" in
  Linux,x86_64) launcher=ImageJ-linux64 ;;
  Linux,*) launcher=ImageJ-linux32 ;;
  Darwin,*) launcher=Contents/MacOS/ImageJ-macosx ;;
  MING*,*) launcher=ImageJ-win32.exe ;;
  *) die "Unknown platform" ;;
esac

# -- Roll out a fresh Fiji --

if [ ! -f fiji-nojre.zip ]
then
  echo
  echo "--> Downloading Fiji"
  curl -fsLO https://downloads.imagej.net/fiji/latest/fiji-nojre.zip
fi

echo "--> Unpacking Fiji"
rm -rf Fiji.app
unzip fiji-nojre.zip

echo
echo "--> Updating Fiji"
Fiji.app/$launcher --update update-force-pristine

echo
echo "--> Copying dependencies into Fiji installation"
(set -x; mvn -Dscijava.app.directory=Fiji.app)
# HACK: Fix mistakes/limitations of the scijava-maven-plugin.
rm -f Fiji.app/jars/*windows*.jar
rm -f Fiji.app/jars/*macosx*.jar
rm -f Fiji.app/jars/*linux*.jar
