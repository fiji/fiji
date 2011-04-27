#!/bin/sh

# A fallback shell script to launch Fiji without the Fiji launcher
# (i.e. when all else fails)

# bend over for SunOS' sh, and use `` instead of $()
DIRECTORY="`dirname "$0"`"
FIJI_ROOT="`cd "$DIRECTORY" && pwd`"

# SunOS's sh cannot do this: FIJI_ROOT="${FIJI_ROOT%*/bin}"
case "$FIJI_ROOT" in
*/bin)
	FIJI_ROOT="`expr "$FIJI_ROOT" : '\(.*\)/bin'`"
	;;
esac

sq_quote () {
	echo "$1" | sed "s/[]\"\'\\\\(){}[\!\$ 	]/\\\\&/g"
}

java_options=
ij_options=
main_class=fiji.Main
dashdash=f
dry_run=

while test $# -gt 0
do
	option="$1"
	case "$dashdash,$option" in
	f,--)
		dashdash=t
		;;
	?,--help)
		cat >&2 << EOF
Usage: $0 [<java-options> --] <options>

Java options are passed to the Java Runtime, ImageJ
options to ImageJ (or Jython, JRuby, ...).

In addition, the following options are supported by Fiji:
General options:
--help, -h
        show this help
--dry-run
	show the command line but do not run anything

Options to run programs other than ImageJ:
--jython
        start Jython instead of ImageJ
--jruby
        start JRuby instead of ImageJ
--clojure
        start Clojure instead of ImageJ
--beanshell, --bsh
	start BeanShell instead of ImageJ
--main-class <class name>
	start the given class instead of ImageJ
--build
	start Fiji's build instead of ImageJ
EOF
		exit 1
		;;
	?,--dry-run)
		dry_run=t
		;;
	?,--jython)
		main_class=org.python.util.jython
		;;
	?,--jruby)
		main_class=org.jruby.Main
		;;
	?,--clojure)
		main_class=clojure.lang.Repl
		;;
	?,--beanshell|?,--bsh)
		main_class=bsh.Interpreter
		;;
	?,--main-class)
		shift
		main_class="$1"
		;;
	?,--main-class=*)
		main_class="`expr "$1" : '--main-class=\(.*\)'`"
		;;
	?,--build)
		main_class=fiji.build.Fake
		;;
	f,*)
		java_options="$java_options `sq_quote "$option"`"
		;;
	t,*)
		ij_options="$ij_options `sq_quote "$option"`"
		;;
	esac
	shift
done

case "$dashdash" in
f)
	ij_options="$java_options"
	java_options=
	;;
esac

case "$main_class" in
fiji.Main|ij.ImageJ)
	ij_options="-port7 $ij_options"
	CLASSPATH="$FIJI_ROOT/jars/Fiji.jar:$FIJI_ROOT/jars/ij.jar"
	;;
*)
	CLASSPATH=
	for path in "$FIJI_ROOT"/jars/*.jar "$FIJI_ROOT"/plugins/*.jar
	do
		CLASSPATH="$CLASSPATH${CLASSPATH:+:}$path"
	done
esac

case "$dry_run" in
t)
	java () {
		printf '%s' java
		i=1
		for option
		do
			printf " \"%s\"" "`sq_quote "$option"`"
			i=`expr $i + 1`
		done
		printf '\n'
	}
	;;
esac

FIJI_ROOT_SQ="`sq_quote "$FIJI_ROOT"`"
EXECUTABLE_NAME="$0"
case "$EXECUTABLE_NAME" in
*/*)
	EXECUTABLE_NAME="`expr "$EXECUTABLE_NAME" : '.*/\([^/]*\)'`"
	;;
esac

eval java -Dpython.cachedir.skip=true \
	-Xincgc -XX:PermSize=128m \
	-Dplugins.dir=$FIJI_ROOT_SQ \
	-Djava.class.path="`sq_quote $CLASSPATH`" \
	-Dsun.java.command=Fiji -Dfiji.dir=$FIJI_ROOT_SQ \
	-Dfiji.executable="`sq_quote "$EXECUTABLE_NAME"`" \
	`cat "$FIJI_ROOT"/jvm.cfg 2> /dev/null` \
	$java_options \
	$main_class $ij_options
