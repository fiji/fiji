#!/bin/sh

# This script is the entry point for the Fiji Build
#
# Call it without parameters to build everything or
# with the filenames of the .jar files to be built

set -a
CWD="$(dirname "$0")" || {
	echo "Huh? Cannot cd to $(dirname "$0")" >&2
	exit 1
}

dirname () {
	case "$1" in
	*/*)
		echo ${1%/*}
		;;
	*\\*)
		echo ${1%\\*}
		;;
	*)
		echo .
		;;
	esac
}

look_for_tools_jar () {
	for d in "$@"
	do
		test -d "$d" || continue
		for j in java default-java
		do
			test -f "$d/$j/lib/tools.jar" || continue
			export TOOLS_JAR="$d/$j/lib/tools.jar"
			return
		done
	done
}

get_java_home () {
	if test -d "$JAVA_HOME"
	then
		echo "$JAVA_HOME"
	else
		if test -n "$java_submodule" && test -d "$CWD/java/$java_submodule"
		then
			echo "$CWD/java/$java_submodule/$(ls -t "$CWD/java/$java_submodule" | head -n 1)/jre"
		fi
	fi
}

need_to_build_fake () {
	(cd "$CWD" &&
	 test -f jars/fake.jar || {
		echo YesPlease
		return
	 }
	 for source in $(find src-plugins/fake/ -name \*.java)
	 do
		test "$source" -nt jars/fake.jar && {
			echo YesPlease
			return
		}
	 done)
	return
}

ensure_fake_is_built () {
	# test whether we need to build it
	test -z "$(need_to_build_fake)" && return

	(cd "$CWD" &&
	 : blow previous builds away
	 rm -rf build/jars/fake/ &&
	 mkdir -p build/jars/fake/ &&
	 : compile classes
	 javac -source 1.5 -target 1.5 -classpath precompiled/javac.jar -d build/jars/fake/ $(find src-plugins/fake/ -name \*.java) &&
	 : compile .jar using Fiji Build
	 java -classpath build/jars/fake/"$PATHSEP"precompiled/javac.jar fiji.build.Fake jars/fake.jar-rebuild ImageJ)
}

PATHSEP=:
case "$(uname -s)" in
Darwin)
	JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Home
	java_submodule=macosx-java3d
	case "$(uname -r)" in
	8.*) platform=tiger;;
	*) platform=macosx;;
	esac; exe=;;
Linux)
	case "$(uname -m)" in
	x86_64)
		platform=linux64
		java_submodule=linux-amd64
		look_for_tools_jar /usr/lib64/jvm /usr/lib/jvm
		;;
	*)	platform=linux32
		java_submodule=linux
		look_for_tools_jar /usr/lib/jvm
		;;
	esac; exe=;;
MINGW*|CYGWIN*)
	CWD="$(cd "$CWD" && pwd)"
	PATHSEP=\;
	case "$PROCESSOR_ARCHITEW6432" in
	'') platform=win32; java_submodule=$platform;;
	*) platform=win64; java_submodule=$platform;;
	esac
	exe=.exe;;
FreeBSD)
	platform=freebsd
	if test -z "$JAVA_HOME"
	then
		JAVA_HOME=/usr/local/jdk1.6.0/jre
		export JAVA_HOME
	fi
	if ! test -f "$JAVA_HOME/jre/lib/ext/vecmath.jar" && ! test -f "$JAVA_HOME/lib/ext/vecmath.jar"
	then
		echo "You are missing Java3D. Please install with"
		echo ""
		echo "        sudo portinstall java3d"
		echo ""
		echo "(This requires some time)"
		exit 1
	fi;;
*)
	platform=
	TOOLS_JAR="$(ls -t /usr/jdk*/lib/tools.jar \
		/usr/local/jdk*/lib/tools.jar 2> /dev/null |
		head -n 1)"
	test -z "$TOOLS_JAR" ||
	export TOOLS_JAR;;
esac


test -n "$platform" &&
test -z "$JAVA_HOME" &&
JAVA_HOME="$(get_java_home)"

# need to clone java submodule
test -z "$platform" ||
test -f "$JAVA_HOME/lib/tools.jar" || test -f "$JAVA_HOME/../lib/tools.jar" ||
test -f "$CWD"/java/"$java_submodule"/Home/lib/ext/vecmath.jar || {
	echo "No JDK found; cloning it"
	JAVA_SUBMODULE=java/$java_submodule
	: jump through hoops to enable a shallow clone of the JDK
	git submodule init "$JAVA_SUBMODULE" && (
		URL="$(git config submodule."$JAVA_SUBMODULE".url)" &&
		case "$URL" in
		contrib@fiji.sc:/srv/git/*)
			URL="git://fiji.sc/${URL#contrib@fiji.sc:/srv/git/}"
			;;
		esac &&
		mkdir -p "$JAVA_SUBMODULE" &&
		cd "$JAVA_SUBMODULE" &&
		git init &&
		git remote add -t master origin "$URL" &&
		git fetch --depth 1 &&
		git reset --hard origin/master
	) || {
		echo "Could not clone JDK" >&2
		exit 1
	}
}

test -n "$JAVA_HOME" &&
test -d "$JAVA_HOME" ||
for d in java/$java_submodule/*
do
	if test -z "$JAVA_HOME" || test "$d" -nt "$JAVA_HOME"
	then
		JAVA_HOME="$d"
	fi
done

if test -d "$JAVA_HOME"
then
	export PATH=$JAVA_HOME/bin:$PATH
fi

: build fake.jar, making sure javac is in the PATH
PATH="$PATH:$(get_java_home)/bin:$(get_java_home)/../bin" \
ensure_fake_is_built || {
	echo "Could not build Fiji Build" >&2
	exit 1
}

# on Win64, with a 32-bit compiler, do not try to compile
case $platform in
win64)
	W64_GCC=/src/mingw-w64/sysroot/bin/x86_64-w64-mingw32-gcc.exe
	test -f "$W64_GCC" && export CC="$W64_GCC"

	case "$CC,$(gcc --version)" in
	,*mingw32*)
		# cannot compile! Fall back to copying
		test "$CWD"/ImageJ.exe -nt "$CWD"/ImageJ.c &&
		test "$CWD"/ImageJ.exe -nt "$CWD"/precompiled/ImageJ-win64.exe &&
		test "$CWD"/ImageJ.exe -nt "$CWD"/Fakefile &&
		test "$CWD"/ImageJ.exe -nt "$CWD"/$jar ||
		cp precompiled/ImageJ-win64.exe ImageJ.exe
	esac
esac

sh "$CWD/bin/ImageJ.sh" --build "$@"
