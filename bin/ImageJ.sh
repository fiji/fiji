#!/bin/sh

# A fallback shell script to launch Fiji without the ImageJ launcher
# (i.e. when all else fails)

# bend over for SunOS' sh, and use `` instead of $()
DIRECTORY="`dirname "$0"`"
PATHSEPARATOR=:
ISWINDOWS=
ISCYGWIN=
case "$(uname -s)" in
MINGW*)
	ISWINDOWS=t
	PATHSEPARATOR=";"
	FIJI_ROOT="$(cd "$DIRECTORY" && pwd -W)"
	;;
CYGWIN*)
	ISWINDOWS=t
	ISCYGWIN=t
	PATHSEPARATOR=";"
	FIJI_ROOT="$(cygpath -d "$(cd "$DIRECTORY" && pwd)" | tr \\\\ /)"
	;;
*)
	FIJI_ROOT="$(cd "$DIRECTORY" && pwd)"
	;;
esac

# SunOS's sh cannot do this: FIJI_ROOT="${FIJI_ROOT%*/bin}"
case "$FIJI_ROOT" in
*/bin)
	FIJI_ROOT="`expr "$FIJI_ROOT" : '\(.*\)/bin'`"
	;;
esac

sq_quote () {
	echo "$1" | sed "s/[]\"\'\\\\(){}[\!\$ 	;]/\\\\&/g"
}

first_java_options=
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
--update
	start Fiji's command-line Updater instead of ImageJ
EOF
		exit 1
		;;
	?,--dry-run)
		dry_run=t
		;;
	?,--headless)
		first_java_options="$first_java_options -Djava.awt.headless=true"
		;;
	?,--mem=*)
		memory=${option#--mem=}
		first_java_options="$first_java_options -Xmx$memory"
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
	?,--update)
		main_class=fiji.updater.Main
		;;
	?,--ant)
		main_class=org.apache.tools.ant.Main
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
	ij_options="$ij_options $java_options"
	java_options=
	;;
esac

get_first () {
	echo "$1"
}

case "$main_class,$(get_first $ij_options)" in
fiji.Main,*.py)
	main_class=org.python.util.jython
	;;
fiji.Main,*.rb)
	main_class=org.jruby.Main
	;;
fiji.Main,*.clj)
	main_class=clojure.lang.Repl
	;;
fiji.Main,*.bsh)
	main_class=bsh.Interpreter
	;;
esac

discover_tools_jar () {
	javac="$(which javac)" &&
	while test -h "$javac"
	do
		javac="$(readlink "$javac")"
	done
	if test -n "$javac"
	then
		JAVA_HOME="${javac%/bin/javac}"
		if test -n "$ISWINDOWS"
		then
			JAVA_HOME="$(cd "$JAVA_HOME" && pwd -W)"
		fi
		export JAVA_HOME
		echo "$JAVA_HOME/lib/tools.jar"
	fi
}

case "$main_class" in
fiji.Main|ij.ImageJ)
	ij_options="$main_class -port7 $ij_options"
	main_class="imagej.ClassLauncher -ijjarpath jars/ -ijjarpath plugins/"
	CLASSPATH="$FIJI_ROOT/jars/ij-launcher.jar$PATHSEPARATOR$FIJI_ROOT/jars/ij.jar$PATHSEPARATOR$FIJI_ROOT/jars/javassist.jar"
	;;
fiji.build.Fake)
	CLASSPATH="$(ls -t $(find $FIJI_ROOT/jars -name fake*.jar) | head -n 1)"
	;;
org.apache.tools.ant.Main)
	CLASSPATH="$(discover_tools_jar)"
	for path in "$FIJI_ROOT"/jars/ant*.jar
	do
		CLASSPATH="$CLASSPATH${CLASSPATH:+$PATHSEPARATOR}$path"
	done
	;;
*)
	CLASSPATH=
	for path in "$FIJI_ROOT"/jars/*.jar "$FIJI_ROOT"/plugins/*.jar
	do
		CLASSPATH="$CLASSPATH${CLASSPATH:+$PATHSEPARATOR}$path"
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

EXT_OPTION=
case "$(uname -s)" in
Darwin)
	EXT_OPTION=-Djava.ext.dirs="$FIJI_ROOT_SQ"/java/macosx-java3d/Home/lib/ext:/Library/Java/Extensions:/System/Library/Java/Extensions:/System/Library/Frameworks/JavaVM.framework/Home/lib/ext
	;;
esac

eval java $EXT_OPTION \
	-Dpython.cachedir.skip=true \
	-Xincgc -XX:PermSize=128m \
	-Dplugins.dir=$FIJI_ROOT_SQ \
	-Djava.class.path="$(sq_quote "$CLASSPATH")" \
	-Dsun.java.command=Fiji -Dij.dir=$FIJI_ROOT_SQ \
	-Dfiji.dir=$FIJI_ROOT_SQ \
	-Dfiji.executable="`sq_quote "$EXECUTABLE_NAME"`" \
	-Dij.executable="`sq_quote "$EXECUTABLE_NAME"`" \
	`cat "$FIJI_ROOT"/jvm.cfg 2> /dev/null` \
	$first_java_options \
	$java_options \
	$main_class $ij_options
