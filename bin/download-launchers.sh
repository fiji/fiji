#!/bin/sh

# This script fetches the ImageJ launcher from http://maven.imagej.net/

set -e
cd "$(dirname "$0")/.."

case "$1" in
release) mode=releases;;
snapshot) mode=snapshots;;
*) echo "Usage: $0 [release|snapshot]" >&2; exit 1;;
esac

groupId=net/imagej
artifactId=ij-launcher
baseurl=http://maven.imagej.net/service/local/repositories/$mode/content/$groupId/$artifactId

get () {
	echo "$metadata" |
	sed -n "s/.*<$1>\(.*\)<\/$1>.*/\1/p" |
	tail -n 1
}

metadata="$(curl $baseurl/maven-metadata.xml)"
version="$(get version)"

baseurl=$baseurl/$version

case "$version" in
*-SNAPSHOT)
	metadata="$(curl $baseurl/maven-metadata.xml)"
	timestamp="$(get timestamp)"
	buildNumber="$(get buildNumber)"
	basename=$artifactId-${version%-SNAPSHOT}-$timestamp-$buildNumber
	;;
*)
	basename=$artifactId-$version
	;;
esac

add_win_logo () {
	wine=
	icon_option=/i
	case "$(uname -s)" in
	MINGW*)
		# work around MINGW's POSIX file path mangling
		icon_option=//i
		;;
	CYGWIN*)
		;;
	Wine-crashes-on-Linux)
		wine=wine
		;;
	*)
		# Need Windows or at least Wine (on Linux) to run RCEDIT
		return
		;;
	esac

	test -e bin/RCEDIT.exe ||
	curl https://raw.github.com/poidasmith/winrun4j/master/org.boris.winrun4j.eclipse/launcher/RCEDIT.exe > bin/RCEDIT.exe

	eval $wine bin/RCEDIT.exe $icon_option "$1" images/fiji.ico
}

tmpdir=.tmp.$$
mkdir $tmpdir

download () {
	case $2 in */*) mkdir -p ${2%/*};; esac
	curl $baseurl/$basename-$1-gcc-executable.nar > $tmpdir/$1.zip
	exe=; case "$2" in *.exe) exe=.exe;; esac
	unzip -p $tmpdir/$1.zip bin/$1-gcc/ij-launcher$exe > $2
	chmod a+x $2
	test -n "$exe" && add_win_logo "$2"
	target=$(echo "$2" | sed 's/ImageJ-/fiji-/')
	case "$target" in fiji-linux32) target=fiji-linux;; esac
	cp $2 $target
}

curl $baseurl/$basename.nar > jars/ij-launcher.jar
./bin/fix-java6-classes.sh jars/ij-launcher.jar

download x86-Windows ImageJ-win32.exe
download amd64-Windows ImageJ-win64.exe

download i386-MacOSX Contents/MacOS/ImageJ-tiger
download x86_64-MacOSX Contents/MacOS/ImageJ-macosx

download i386-Linux ImageJ-linux32
download amd64-Linux ImageJ-linux64

rm -rf $tmpdir
