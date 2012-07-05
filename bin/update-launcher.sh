#!/bin/sh

# This script uses the Fiji Updater to update the ImageJ launcher hence the
# launcher cannot be used to launch the Updater to do the job.
#
# Additionally, this script needs to know about the default location and
# suffix to be able to install the launcher under the name ImageJ{.exe}

prefix=
suffix=
exe=
case "$(uname -s)" in
Darwin)
	prefix=Contents/MacOS/
	case "$(uname -r)" in
	8.*)
		suffix=-tiger
		;;
	*)
		suffix=-macosx
		;;
	esac
	;;
Linux)
	case "$(uname -m)" in
	x86_64)
		suffix=-linux64
		;;
	*)
		suffix=-linux32
		;;
	esac
	;;
MINGW*|CYGWIN*)
	exe=.exe
	case "$PROCESSOR_ARCHITEW6432" in
	'')
		suffix=-win32
		;;
	*)
		suffix=-win64
		;;
	esac
	;;
esac &&
(cd "$(dirname "$0")"/.. &&
 case "$suffix" in
 '')
	cp bin/ImageJ.sh ImageJ
	;;
 *)
	test -f plugins/Fiji_Updater.jar || ./Build.sh plugins/Fiji_Updater.jar
	file=${prefix}ImageJ$suffix$exe &&
	bin/ImageJ.sh --update update $file &&
	cp $file ImageJ$exe
	;;
 esac)
