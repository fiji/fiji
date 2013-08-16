#!/bin/sh

# This script is the entry point for the Fiji Build
#
# Call it without parameters to build everything or
# with the filenames of the .jar files to be built

set -a
unset CDPATH
CWD="$(dirname "$0")" || {
	echo "Huh? Cannot cd to $(dirname "$0")" >&2
	exit 1
}

# MinGW does not necessarily have dirname.exe

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

# platform-specific stuff

PATHSEP=:
UNAME_S="$(uname -s)"
LAUNCHER=bin/ImageJ.sh
FIJILAUNCHER=
case "$UNAME_S" in
Darwin)
	JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Home
	java_submodule=macosx-java3d
	case "$(uname -r)" in
	8.*) platform=tiger;;
	*) platform=macosx;;
	esac
	exe=
	LAUNCHER=Contents/MacOS/ImageJ-$platform
	FIJILAUNCHER=Contents/MacOS/fiji-$platform
	;;
Linux)
	case "$(uname -m)" in
	x86_64)
		platform=linux64
		java_submodule=linux-amd64
		;;
	*)	platform=linux32
		java_submodule=linux
		;;
	esac
	exe=
	LAUNCHER=ImageJ-$platform
	FIJILAUNCHER=fiji-$platform
	FIJILAUNCHER=${FIJILAUNCHER%32}
	;;
MINGW*|CYGWIN*)
	CWD="$(cd "$CWD" && pwd)"
	PATHSEP=\;
	case "$PROCESSOR_ARCHITEW6432" in
	'') platform=win32; java_submodule=$platform;;
	*) platform=win64; java_submodule=$platform;;
	esac
	exe=.exe
	LAUNCHER=ImageJ-$platform.exe
	FIJILAUNCHER=fiji-$platform.exe
	;;
FreeBSD)
	platform=freebsd
	if test -z "$JAVA_HOME"
	then
		JAVA_HOME=/usr/local/jdk1.6.0/jre
		export JAVA_HOME
	fi
	if ! test -f "$JAVA_HOME/jre/lib/ext/vecmath.jar" &&
		! test -f "$JAVA_HOME/lib/ext/vecmath.jar"
	then
		echo "You are missing Java3D. Please install with"
		echo ""
		echo "        sudo portinstall java3d"
		echo ""
		echo "(This requires some time)"
		exit 1
	fi
	;;
*)
	platform=
	TOOLS_JAR="$(ls -t /usr/jdk*/lib/tools.jar \
		/usr/local/jdk*/lib/tools.jar 2> /dev/null |
		head -n 1)"
	test -z "$TOOLS_JAR" ||
	export TOOLS_JAR
	;;
esac

# Java

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

case "$JAVA_HOME" in
[A-Z]:*)
	# assume this is MSys
	JAVA_HOME="$(cd "$JAVA_HOME" && pwd)" ||
	unset JAVA_HOME
	;;
esac

test -n "$JAVA_HOME" &&
test -d "$JAVA_HOME" ||
for d in java/$java_submodule/*
do
	test "$d/jre" || continue
	if test -z "$JAVA_HOME" || test "$d" -nt "$JAVA_HOME"
	then
		JAVA_HOME="$CWD/$d/jre"
	fi
done

if test -d "$JAVA_HOME"
then
	if test -d "$JAVA_HOME/jre"
	then
		JAVA_HOME="$JAVA_HOME/jre"
	fi
	export PATH="$JAVA_HOME/bin:$PATH"
fi

# make sure java is in the PATH
PATH="$PATH:$(get_java_home)/bin:$(get_java_home)/../bin"
export PATH

# Thanks, MacOSX (or for that matter, BSD), and Windows, for easy, standard
# ways to get the mtime of a file.

get_mtime () {
	stat -c %Y "$1"
}

# JAVA_HOME needs to be a DOS path for Windows from here on
case "$UNAME_S" in
MINGW*)
	export JAVA_HOME="$(cd "$JAVA_HOME" && pwd -W)"
	get_mtime () {
		date -r "$1" +%s
	}
	;;
CYGWIN*)
	export JAVA_HOME="$(cygpath -d "$JAVA_HOME")"
	;;
Darwin*)
	get_mtime () {
		stat -f %m "$1"
	}
	;;
esac

# Figure out whether $2 (the destination) is newer than $1 (the source).
# If $2 is a SNAPSHOT .jar, must not be older than a day, either.
#
# This function is mainly used to test whether something is older than
# Build.sh and hence needs to be updated.

uptodate () {
	test -f "$2" &&
	test "$2" -nt "$1" &&
	case "$2" in
	*-SNAPSHOT.jar)
		test "$(($(get_mtime "$2")-$(date +%s)))" -gt -86400
		;;
	esac
}

# we need an absolute CWD from now on
case "$CWD" in
[A-Z]:*|/*)
	# is already absolute
	;;
*)
	CWD="$(cd "$CWD" && pwd)"
	;;
esac

# pseudo-Maven (thanks to SciJava's maven-helper)

ARGV0="$CWD/$0"
SCIJAVA_COMMON="$CWD/modules/scijava-common"
MAVEN_HELPER="$SCIJAVA_COMMON/bin/maven-helper.sh"
force_update=
maven_helper () {
	uptodate "$ARGV0" "$MAVEN_HELPER" || {
		force_update=t
		if test -d "$SCIJAVA_COMMON/.git"
		then
			(cd "$SCIJAVA_COMMON" &&
			 test arefs/heads/master != "a$(git rev-parse --symbolic-full-name HEAD)" ||
			 git pull -k) >&2
		else
			git clone https://github.com/scijava/scijava-common \
				"$SCIJAVA_COMMON" >&2
		fi || {
			echo "Could not update SciJava-common" >&2
			exit 1
		}
		if test ! -f "$MAVEN_HELPER"
		then
			echo "Could not find $MAVEN_HELPER!" >&2
			exit 1
		fi
		touch "$MAVEN_HELPER"
	}
	test $# = 0 ||
	sh -$- "$MAVEN_HELPER" "$@"
}

maven_update () {
	for gav in "$@"
	do
		artifactId="${gav#*:}"
		version="${artifactId#*:}"
		artifactId="${artifactId%%:*}"
		path="jars/$artifactId-$version.jar"

		test -z "$force_update" ||
		rm -f "$path"

		(cd "$CWD"
		 test -f jars/"$artifactId".jar && rm jars/"$artifactId".jar
		 for file in jars/"$artifactId"-[0-9]*.jar
		 do
			test "a$file" = a"$path" && continue
			test -f "$file" || continue
			rm "$file"
		 done

		 uptodate "$ARGV0" "$path" && continue
		 echo "Downloading $gav" >&2
		 (cd jars/ && maven_helper install "$gav")
		 if test ! -f "$path"
		 then
			echo "Failure to download $path" >&2
			exit 1
		 fi
		 uptodate "$ARGV0" "$path" ||
		 touch "$path")
	done
}

update_launcher () {
	case "$LAUNCHER" in
	bin/ImageJ.sh)
		test -z "$exe" || rm -f "$CWD/fiji$exe"
		uptodate "$ARGV0" "$CWD/fiji" || {
			cat > "$CWD/fiji" << EOF
#!/bin/sh

exec "$CWD/$LAUNCHER" "$@"
EOF
			chmod a+x "$CWD/fiji"
		}
		;;
	*)
		uptodate "$ARGV0" "$CWD/$LAUNCHER" ||
		(cd $CWD &&
		 sh -$- bin/download-launchers.sh release $platform)
		;;
	esac
	test -z "$FIJILAUNCHER" ||
	test ! -f "$CWD/$FIJILAUNCHER" ||
	rm "$CWD/$FIJILAUNCHER"
}

# make sure that javac and ij-minimaven are up-to-date

FIJI_VERSION="$(maven_helper property-from-pom "$CWD"/pom.xml fiji.version)"
IMAGEJ_VERSION="$(maven_helper property-from-pom "$CWD"/pom.xml imagej.version)"
maven_update sc.fiji:javac:$FIJI_VERSION \
	net.imagej:ij-minimaven:$IMAGEJ_VERSION

# command-line options

OPTIONS="-Dimagej.app.directory=\"$CWD\""
while test $# -gt 0
do
	case "$1" in
	verbose=*)
		OPTIONS="$OPTIONS -Dminimaven.verbose=true"
		;;
	-D*)
		OPTIONS="$OPTIONS $1"
		;;
	*=*)
		OPTIONS="$OPTIONS -D$1"
		;;
	--)
		shift
		break
		;;
	-*)
		echo "Invalid option: $1" >&2
		exit 1
		;;
	*)
		break
		;;
	esac
	shift
done

# handle targets

if test $# = 0
then
	eval sh -$- "$CWD/bin/ImageJ.sh" --mini-maven "$OPTIONS" install
	update_launcher
	for name in fiji ImageJ
	do
		uptodate "$LAUNCHER" "$name$exe" ||
		cp "$LAUNCHER" "$name$exe"
	done
else
	for name in "$@"
	do
		case "$name" in
		fiji|ImageJ)
			update_launcher
			uptodate "$LAUNCHER" "$name$exe" ||
			cp "$LAUNCHER" "$name$exe"
			continue
			;;
		clean)
			eval sh -$- \"$CWD/bin/ImageJ.sh\" --mini-maven \
                                "$OPTIONS" clean
			continue
			;;
		esac
		artifactId="${name##*/}"
		artifactId="${artifactId%.jar}"
		artifactId="${artifactId%%-[0-9]*}"
		case "$name" in
		*-rebuild)
			artifactId=${artifactId%-rebuild}
			artifactId="${artifactId%.jar}"
			eval sh -$- "$CWD/bin/ImageJ.sh" --mini-maven \
				"$OPTIONS" -DartifactId="$artifactId" -- clean
			;;
		esac
		eval sh -$- "$CWD/bin/ImageJ.sh" --mini-maven \
			"$OPTIONS" -DartifactId="$artifactId" -- install
	done
fi
