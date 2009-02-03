#!/bin/sh

case "$(uname -s)" in
MINGW*)
	dirname () {
		case "$1" in
		*\\*|*/*)
			echo "$1" | sed 's/[\\\/][^\\\/]*$//'
			;;
		*)
			echo .
			;;
		esac
	}
	;;
esac

CWD="$(dirname "$0")"

case "$(uname -s)" in
Darwin) platform=macosx; exe=;;
Linux)
	case "$(uname -m)" in
		x86_64) platform=linux64;;
		*) platform=linux;;
	esac; exe=;;
MINGW*|CYGWIN*) platform=win32; exe=.exe;;
esac

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

# make sure fake.jar is up-to-date
test "a$targets" != afake.jar &&
test ! -f "$CWD"/fake.jar -o "$CWD"/fake/Fake.java -nt "$CWD"/fake.jar && {
	sh "$0" $variables fake.jar || exit
}

# make sure the Fiji launcher is up-to-date
test "a$targets" != afake.jar -a "a$targets" != afiji &&
test ! -f "$CWD"/fiji -o "$CWD"/fiji.cxx -nt "$CWD"/fiji$exe && {
	sh "$0" $variables fiji || exit
}

# still needed for Windows, which cannot overwrite files that are in use
test -f "$CWD"/fiji$exe -a -f "$CWD"/fake.jar &&
test "a$targets" != afake.jar -a "a$targets" != afiji &&
exec "$CWD"/fiji$exe --fake "$@"

# fall back to precompiled
test -f "$CWD"/precompiled/fiji-$platform$exe \
	-a -f "$CWD"/precompiled/fake.jar &&
exec "$CWD"/precompiled/fiji-$platform$exe --fake -- "$@"

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
    fi
fi

# fall back to calling Fake with system Java
test -f "$CWD"/fake.jar &&
$SYSTEM_JAVA -classpath "$CWD"/fake.jar Fake "$@"

# fall back to calling precompiled Fake with system Java
test -f "$CWD"/precompiled/fake.jar &&
$SYSTEM_JAVA -classpath "$CWD"/precompiled/fake.jar Fake "$@"

# fall back to compiling and running with system Java
$SYSTEM_JAVAC -source 1.3 -target 1.3 "$CWD"/fake/Fake.java &&
$SYSTEM_JAVA -classpath "$CWD"/fake Fake "$@"
