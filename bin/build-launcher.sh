#!/bin/sh

# This builds the ImageJ launcher from the IJ2 sources and copies it into place

set -e
cd "$(dirname "$0")/.."

debugoption=
case "$1" in
-g|-d|--debug)
	debugoption=-Ddebug.option=-g
	;;
esac

exe=
mvnopts=
macprefix=
case "$(uname -s)" in
Darwin)
	os=MacOSX
	macprefix=Contents/MacOS/
	case "$(uname -r)" in
	8.*)
		platform=tiger
		arch=i386
		mvnopts="-Dos.arch=i386 -P MacOSX-Tiger -P '!MacOSX-Leopard'"
		;;
	*)
		platform=macosx
		arch=x86_64
		;;
	esac
	;;
Linux)
	os=Linux
	case "$(uname -m)" in
	x86_64)
		platform=linux64
		arch=amd64
		;;
	*)
		platform=linux
		arch=i386
		;;
	esac
	;;
MINGW*|CYGWIN*)
	os=Windows
	exe=.exe
	case "$PROCESSOR_ARCHITEW6432" in
	'')
		platform=win32
		arch=x86
		arch=i386
		;;
	*)
		platform=win64
		arch=amd64
		;;
	esac
	;;
esac

path=modules/imagej2/core/launcher
artifactId=ij-launcher
version=$(sed -n 's-.*<version>\([^${}]*\)</version>.*-\1-p' < $path/pom.xml)

IJDIR="$(pwd)"
eval ./bin/maven.sh $mvnopts $debugoption -f $path/pom.xml &&
cp $path/target/nar/$artifactId-$version-$arch-$os-gcc-executable/bin/$arch-$os-gcc/$artifactId ${macprefix}ImageJ-$platform$exe &&

cp ${macprefix}ImageJ-$platform$exe ${macprefix}fiji-$platform$exe &&

cp $path/target/$artifactId-$version.jar jars/$artifactId.jar
