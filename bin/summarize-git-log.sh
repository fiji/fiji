#!/bin/sh

cd "$(dirname "$0")"/..

test $# = 0 && {
	cat << EOF >&2
Usage: $0 <git-log-options>

Example:

	$0 Fiji-Heidelberg..
EOF
	exit 1
}

# Fix mismerge

revert=fe66dbf5e9afafd6e93a4b327f9f55c0b04ae19b
parent=b408de6c3dcf666e254c6fec6893d9109a2c8819
test $(git rev-parse $revert^) != $(git rev-parse $parent^) &&
git replace $revert $parent

# Numbers

get_numbers () {
	added=0
	removed=0
	git log --no-merges --numstat "$@" |
	grep "^[0-9]" |
	sed -n 's/^\([0-9]\+\)[^0-9]*\([0-9]\+\).*/\1 \2/p' | {
		while read add remove
		do
			added=$(($added+$add))
			removed=$(($removed+$remove))
		done
		echo "$added $removed"
	}
}

get_authors () {
	git shortlog --no-merges -n -s "$@" |
	cut -c 8-
}

(
	numbers=$(get_numbers "$@")
	added=${numbers% *}
	removed=${numbers#* }
	echo "There have been $added lines added and $removed lines removed,"

	# Authors:

	echo "with the help of (in alphabetical order):"

	get_authors "$@" |
	sed -e 's/$/,/'

	echo "and many other helpers.") |
fmt -76

files=$(git log --no-merges --format=%% --name-only "$@" |
	grep -ve '^$' -e '^%$' -e '^staged-plugins/' -e '^tests/' -e '^\\.' \
		-e '/\.' -e '^Fakefile' -e '^TODO' -e '^RELEASE-NOTES' |
	cut -d / -f1-2 |
	sort |
	uniq)

for f in $files
do
	case "$f" in
	modules/*)
		f="$f ${f#modules/}"
		;;
	*/*)
		;;
	*)
		test -d modules/"$f" &&
		continue # avoid double-listing
		;;
	esac

	log="$(git log --no-merges --format="%s" --reverse "$@" -- $f)"
	test -z "$log" || {
		case "$f" in
		ImageJ.c)
			f="ImageJ Launcher"
			;;
		src-plugins/*)
			f=${f#src-plugins/}
			;;
		modules/*)
			f="Submodule '${f#* }'"
			;;
		esac
		printf "\n$f:\n"
		echo "$log" |
		sed 's/^/    /'
	}
done
