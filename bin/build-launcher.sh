#!/bin/sh

# This builds the ImageJ launcher from the IJ2 sources and copies it into place

set -e
cd "$(dirname "$0")/.."

debugoption=
force32=
while test $# -gt 0
do
	case "$1" in
	-g|-d|--debug)
		debugoption=-Ddebug.option=-g
		;;
	-32)
		force32=t
		;;
	*)
		echo "Unknown option: $1" >&2
		exit 1
		;;
	esac
	shift
done

exe=
mvnopts=
macprefix=
case "$(uname -s)" in
Darwin)
	os=MacOSX
	macprefix=Contents/MacOS/
	mkdir -p $macprefix
	case "$force32,$(uname -r)" in
	t,*|,8.*)
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
	case "$force32,$(uname -m)" in
	,x86_64)
		platform=linux64
		arch=amd64
		;;
	*)
		platform=linux
		arch=i386
		mvnopts="-Dos.arch=i386 -P i386-Linux -P !amd64-Linux"
		;;
	esac
	;;
MINGW*|CYGWIN*)
	os=Windows
	exe=.exe
	case "$force32,$PROCESSOR_ARCHITEW6432" in
	t,*|,)
		platform=win32
		arch=x86
		mvnopts="-Dos.arch=$arch"
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
version=$(sed -n 's-.*<version>\([^${}]*\)</version>.*-\1-p' < $path/pom.xml | head -n 1)

IJDIR="$(pwd)"
eval ./bin/maven.sh $mvnopts $debugoption -f $path/pom.xml &&
cp $path/target/nar/$artifactId-$version-$arch-$os-gcc-executable/bin/$arch-$os-gcc/$artifactId ${macprefix}ImageJ-$platform$exe &&

cp ${macprefix}ImageJ-$platform$exe ${macprefix}fiji-$platform$exe &&
cp ${macprefix}ImageJ-$platform$exe ${macprefix}ImageJ$exe &&
cp ${macprefix}ImageJ-$platform$exe ${macprefix}fiji$exe &&

cp $path/target/$artifactId-$version.jar jars/$artifactId-$version.jar
