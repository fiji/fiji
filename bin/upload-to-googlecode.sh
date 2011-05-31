#!/bin/sh

URL=http://fiji.sc

DOWNLOADS=$(curl $URL/wiki/index.php/Downloads |
	sed -n 's|^.*a href="'$URL'/downloads/\([^/]*\).*$|\1|p' |
	sort |
	uniq |
	sed 's|^|/var/www/downloads/|')

# GoogleCode
googlecode_pwd="$(cat "$(dirname "$0")"/.googlecode-pwd)"
for path in $DOWNLOADS/*
do
	test -d "$path" && continue
	test $(stat -c %s "$path") -gt $((100*1024*1024)) && continue

	f="$(basename "$path")"
	case $f in
	fiji-all-*)
		summary="Fiji for all supported platforms"
		;;
	fiji-nojre-*)
		summary="Fiji for all supported platforms, without Java"
		;;
	fiji-live-*)
		summary="Fiji Live CD image"
		;;
	fiji-usb-*)
		summary="Fiji USB stick image"
		;;
	fiji-linux-*)
		summary="Fiji for Linux (32-bit AMD/Intel)"
		;;
	fiji-linux64-*)
		summary="Fiji for Linux (64-bit AMD/Intel)"
		;;
	fiji-win32-*)
		summary="Fiji for Windows (32-bit AMD/Intel)"
		;;
	fiji-win64-*)
		summary="Fiji for Windows (64-bit AMD/Intel)"
		;;
	fiji-macosx-*)
		summary="Fiji for MacOSX (32-bit/64-bit AMD/Intel, 32-bit PowerPC)"
		;;
	*)
		summary="$(basename $f)"
		;;
	esac

	echo "Uploading $path..."
	python "$(dirname "$0")"/googlecode_upload.py -s "$summary" -p fiji-bi \
		-u johannes.schindelin@gmail.com -w "$googlecode_pwd" "$path"
done
