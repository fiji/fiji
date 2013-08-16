#!/bin/sh

# This script fetches the ImageJ launcher from http://maven.imagej.net/

set -e
unset CDPATH
cd "$(dirname "$0")/.."

case "$1" in
release) mode=releases;;
snapshot) mode=snapshots;;
*) echo "Usage: $0 [release|snapshot]" >&2; exit 1;;
esac
platform="$2"
case "$platform" in
osx*)
	platform=macosx
	;;
esac

groupId=net/imagej
artifactId=ij-launcher
baseurl=http://maven.imagej.net/content/repositories/$mode/$groupId/$artifactId

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
	case "$(uname -s)" in
	MINGW*|CYGWIN*)
		cp "$1" debug.exe
		./debug.exe --set-icon "$1" images/fiji.ico
		;;
	Linux)
		wine "$1" --set-icon "$1" images/fiji.ico
		;;
	esac

}

tmpdir=.tmp.$$
mkdir $tmpdir

download () {
	exe=; case "$2" in *.exe) exe=.exe;; esac

	case $2 in */*) mkdir -p ${2%/*};; esac
	curl $baseurl/$basename-$1-gcc-executable.nar > $tmpdir/$1.zip
	unzip -p $tmpdir/$1.zip bin/$1-gcc/ij-launcher$exe > $2
	chmod a+x $2

	case $2,$mode in
	*.exe,snapshots)
		# Jenkins provided beautiful .exe files with icons for us
		fiji=fiji-${2#ImageJ-}
		curl -O http://jenkins.imagej.net/view/ImageJ/job/Windows-Fiji-launcher-with-icons/label=Windows/lastSuccessfulBuild/artifact/$fiji
		;;
	*)
		fiji=$(echo "$2" | sed 's/ImageJ-/fiji-/')
		case "$fiji" in fiji-linux32) fiji=fiji-linux;; esac
		cp $2 $fiji
		;;
	esac
	if test ! -z "$platform"
	then
		cp $2 ImageJ$exe
		cp $fiji fiji$exe
	fi
}

download_platform () {
	case "$1" in
	win32)
		download x86-Windows ImageJ-win32.exe
		;;
	win64)
		download amd64-Windows ImageJ-win64.exe
		;;
	tiger)
		download i386-MacOSX Contents/MacOS/ImageJ-tiger
		;;
	macosx)
		download x86_64-MacOSX Contents/MacOS/ImageJ-macosx
		;;
	linux)
		download i386-Linux ImageJ-linux32
		;;
	linux32)
		download i386-Linux ImageJ-linux32
		;;
	linux64)
		download amd64-Linux ImageJ-linux64
		;;
	*)
		echo "Unknown platform: $1" >&2
		exit 1
		;;
	esac
}

curl $baseurl/$basename.nar > jars/ij-launcher-$version.jar

if test -z "$platform"
then
	for p in win32 win64 tiger macosx linux32 linux64
	do
		download_platform $p
	done
else
	download_platform "$platform"
fi

rm -rf $tmpdir
