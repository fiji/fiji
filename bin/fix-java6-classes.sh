#!/bin/sh

cd "$(dirname "$0")/.."

die () {
	echo "$*" >&2
	exit 1
}

case $# in
0)
	OFFENDERS=$(./fiji tests/class_versions.py  |
		sed -n -e 's/(.*//' -e 's/^\t//p' |
		uniq)
	;;
*)
	OFFENDERS="$*"
	;;
esac

for f in $OFFENDERS
do
	echo "Fixing $f..."
	case $f in
	*.jar)
		./fiji --jar retro/retrotranslator-transformer-1.2.7.jar \
			-srcjar $f -destjar $f.new -target 1.5 &&
		mv $f.new $f
		;;
	*.class)
		TMPDIR=$(mktemp -d) &&
		mv $f $TMPDIR &&
		./fiji --jar retro/retrotranslator-transformer-1.2.7.jar \
			-srcdir $TMPDIR -destdir $(dirname $f) -target 1.5 &&
		rm -r $TMPDIR
		;;
	*)
		die "Unknown type: $f"
		;;
	esac ||
	die "Could not transform $f"
done
