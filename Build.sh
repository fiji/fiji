#!/bin/sh


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
			test -f "$d/$java/lib/tools.jar" || continue
			export TOOLS_JAR="$d/$java/lib/tools.jar"
			return
		done
	done
}

CWD="$(dirname "$0")"

case "$(uname -s)" in
Darwin)
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
		look_for_tools_jar /usr/lib/jvm
		;;
	*)	platform=linux32
		java_submodule=linux
		look_for_tools_jar /usr/lib64/jvm
		;;
	esac; exe=;;
MINGW*|CYGWIN*)
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
	# copy and use bin/ImageJ-other.sh
	test -f "$CWD/ImageJ" &&
	test "$CWD/bin/ImageJ" -nt "$CWD/bin/ImageJ-other.sh" ||
	cp "$CWD/bin/ImageJ-other.sh" "$CWD/ImageJ"
	TOOLS_JAR="$(ls -t /usr/jdk*/lib/tools.jar \
		/usr/local/jdk*/lib/tools.jar 2> /dev/null |
		head -n 1)"
	test -z "$TOOLS_JAR" ||
	export TOOLS_JAR;;
esac

test -n "$platform" &&
test -z "$JAVA_HOME" &&
JAVA_HOME="$("$CWD"/precompiled/ImageJ-"$platform" --print-java-home 2> /dev/null)"

if test -n "$platform" && test ! -d "$JAVA_HOME"
then
	JAVA_HOME="$CWD"/java/$java_submodule
	JAVA_HOME="$JAVA_HOME"/"$(ls -t "$JAVA_HOME" | head -n 1)"
fi

# need to clone java submodule
test -z "$platform" ||
test -f "$JAVA_HOME/lib/tools.jar" || test -f "$JAVA_HOME/../lib/tools.jar" ||
test -f "$CWD"/java/"$java_submodule"/Home/lib/ext/vecmath.jar || {
	echo "No JDK found; cloning it"
	JAVA_SUBMODULE=java/$java_submodule
	git submodule init "$JAVA_SUBMODULE" && (
		URL="$(git config submodule."$JAVA_SUBMODULE".url)" &&
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

handle_variables () {
	case "$1" in
	--strip) strip_variables=t; shift;;
	*) strip_variables=;;
	esac
	while test $# -ge 1
	do
		case "$1" in
		*=*) test ! -z "$strip_variables" || echo "$1";;
		*) test -z "$strip_variables" || echo "$1";;
		esac
		shift
	done
}

targets=$(handle_variables --strip "$@")
variables=$(handle_variables "$@")

jar=jars/fake.jar
pre_jar=precompiled/${jar##*/}
source_dir=src-plugins/fake
sources=$source_dir/fiji/build/*.java

jarUpToDate () {
	test -f "$CWD/$jar" || return 1
	for source in $sources
	do
		test "$CWD/$source" -nt "$CWD/$jar" && return 1
	done
	return 0
}

# make sure fake.jar is up-to-date
test "a$targets" != a$jar &&
! jarUpToDate && {
	(cd "$CWD" && sh "$(basename "$0")" $variables $jar) || exit
}

# make sure the ImageJ launcher is up-to-date
test "a$targets" != a$jar -a "a$targets" != aImageJ &&
test ! -f "$CWD"/ImageJ -o "$CWD"/ImageJ.c -nt "$CWD"/ImageJ$exe && {
	(cd "$CWD" && sh "$(basename "$0")" $variables ImageJ) || exit
}

# on Win64, with a 32-bit compiler, do not try to compile
case $platform in
win64)
	W64_GCC=/src/mingw-w64/sysroot/bin/x86_64-w64-mingw32-gcc.exe
	test -f "$W64_GCC" && export CC="$W64_GCC"

	case "$CC,$(gcc --version)" in
	,*mingw32*)
		# cannot compile!
		test "$CWD"/ImageJ.exe -nt "$CWD"/ImageJ.c &&
		test "$CWD"/ImageJ.exe -nt "$CWD"/precompiled/ImageJ-win64.exe &&
		test "$CWD"/ImageJ.exe -nt "$CWD"/Fakefile &&
		test "$CWD"/ImageJ.exe -nt "$CWD"/$jar ||
		cp precompiled/ImageJ-win64.exe ImageJ.exe
	esac
esac

# still needed for Windows, which cannot overwrite files that are in use
test -f "$CWD"/ImageJ$exe -a -f "$CWD"/$jar &&
test "a$targets" != a$jar -a "a$targets" != aImageJ &&
exec "$CWD"/ImageJ$exe --build "$@"

# fall back to precompiled
test -f "$CWD"/precompiled/ImageJ-$platform$exe \
	-a -f "$CWD"/precompiled/${jar##*/} &&
exec "$CWD"/precompiled/ImageJ-$platform$exe --build -- "$@"

export SYSTEM_JAVA=java
export SYSTEM_JAVAC=javac

# If JAVA_HOME leads to an executable java or javac then use them:
if [ x != x$JAVA_HOME ]
then
    if [ -e $JAVA_HOME/bin/java ]
    then
        export SYSTEM_JAVA=$JAVA_HOME/bin/java
    fi
    if [ -e $JAVA_HOME/bin/javac ]
    then
        export SYSTEM_JAVAC=$JAVA_HOME/bin/javac
    elif [ -e $JAVA_HOME/../bin/javac ]
    then
        export SYSTEM_JAVAC=$JAVA_HOME/../bin/javac

    fi
fi

# fall back to calling Fake with system Java
test -f "$CWD"/$jar &&
$SYSTEM_JAVA -classpath "$CWD"/$jar fiji.build.Fake "$@"

# fall back to calling precompiled Fake with system Java
test -f "$CWD"/$pre_jar &&
$SYSTEM_JAVA -classpath "$CWD"/$pre_jar fiji.build.Fake "$@"

# fall back to compiling and running with system Java
mkdir -p "$CWD"/build &&
$SYSTEM_JAVAC -d "$CWD"/build/ "$CWD"/$sources &&
$SYSTEM_JAVA -classpath "$CWD"/build fiji.build.Fake "$@"
