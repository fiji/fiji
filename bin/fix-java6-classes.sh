#!/bin/sh

FIJIROOT="$(dirname "$0")/.."
FIJI="$FIJIROOT"/fiji
RETRO="$FIJIROOT"/retro/retrotranslator-transformer-1.2.7.jar

die () {
	echo "$*" >&2
	exit 1
}

case $# in
0)
	OFFENDERS=$(cd "$FIJIROOT" && ./fiji tests/class_versions.py |
		sed -n -e 's/(.*//' -e 's/^\t//p' |
		uniq)
	;;
*)
	OFFENDERS="$*"
	;;
esac

TMPDIR="$(mktemp -d)"
for f in $OFFENDERS
do
	echo "Fixing $f..."
	case "$f" in
	*.jar)
		"$FIJI" --jar "$RETRO" \
			-srcjar "$f" -destjar "$f".new -target 1.5 &&
		mv -f "$f".new "$f"
		;;
	*.class)
		mv "$f" "$TMPDIR" &&
		"$FIJI" --jar "$RETRO" \
			-srcdir "$TMPDIR" -destdir $(dirname "$f") -target 1.5
		;;
	*)
		die "Unknown type: $f"
		;;
	esac ||
	die "Could not transform $f"
done
rm -rf "$TMPDIR"
