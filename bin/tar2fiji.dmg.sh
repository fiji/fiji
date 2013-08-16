#!/bin/sh

set -e

die () {
	echo "$*" >&2
	exit 1
}

test Darwin = "$(uname -s)" || die "This script only runs on MacOSX"

if test $# != 2
then
	die "Usage: $0 (-|<tar>) <dmg>"
fi

tar="$1"
dmg="$2"

test -f "$dmg" && die "$dmg already exists"

get_disk_id () {
	hdid "$1" |
	sed -n 's|.*/dev/\([^ ]*\)[^/]*Apple_HFS.*|\1|p'
}

get_folder () {
	hdid "$1" |
	sed -n 's|.*Apple_HFS[ 	]*\([^]*\).*|\1|p'
}

eject () {
	disk_id="$(get_disk_id "$1")"
	echo "disk_id: $disk_id"
	hdiutil eject "$disk_id"
}

# make a temporary directory and extract the tar into that directory
tmp="$(mktemp -d fiji-dmgXXXXXXX)"
cat "$tar" | (cd "$tmp" && tar xvf -)

# create temporary disk image and format, ejecting when done
hdiutil create "$dmg" -srcfolder "$tmp" \
	-fs HFS+ -format UDRW -volname Fiji -ov
folder="$(get_folder "$dmg")"
echo "folder: $folder"
rm -r "$tmp"

# arrange icons and copy background image
(cd "$(dirname "$0")"/..
test -f resources/install-fiji.jpg ||
./fiji bin/generate-finder-background.py
cp resources/install-fiji.jpg "$folder"/.background.jpg
ln -s /Applications "$folder"/Applications
VERSIONER_PERL_PREFER_32_BIT=yes \
perl bin/generate-finder-dsstore.perl "$folder"
)

# pack the .dmg
eject "$dmg"
mv "$dmg" "$dmg".tmp
hdiutil convert "$dmg".tmp -format UDZO -o "$dmg"
eject "$dmg"
rm "$dmg".tmp

rm -rf "$tmp"
