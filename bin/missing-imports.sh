#!/bin/sh

die () {
	echo "$*" >&2
	exit 1
}

test $# = 1 ||
die "Usage: $0 <file>"

path="$1"

candidates="$(cat "$path" |
	grep -v ^from |
	tr ' 	,().;:[]' '\n' |
	grep '^[A-Z]' |
	grep -v '"' |
	sort |
	uniq)"

case "$path" in
*.py)
	imported="$(cat "$path" |
		sed -n 's/^from .* import //p' |
		tr ' ,' '\n' |
		grep -v '^$' |
		sort |
		uniq)"
	;;
*.bsh)
	imported="$(cat "$path" |
		sed -n 's/^import \(.*\.\)\(.*\);/\2/p' |
		sort |
		uniq)"
	;;
*)
	die "Unhandled language: $path"
	;;
esac

candidates="$(printf "%s\n%s\n%s\n" "$candidates" "$imported" "$imported" |
	sort |
	uniq -u)"

ij1classes="$(tar tf jars/ij-1*.jar |
	sed -n 's/\.class$//p' |
	grep -v '/ColorPanel$' |
	tr '/' '.' |
	sort |
	uniq)"

ij1classnames="$(echo "$ij1classes" |
	sed 's/.*\.//')"

missing="$(printf "%s\n%s\n%s\n" "$candidates" "$ij1classnames" |
	sort |
	uniq -d)"

test -n "$missing" ||
exit 0

toimport="$(echo "$ij1classes" |
	eval grep $(echo "$missing" |
		sed -e 's/\$/\\&/g' -e 's/.*/-e \\\\.&$/'))"

case "$path" in
*.py)
	imports="$(p=; c=; echo "$toimport" |
		while read class
		do
			c1="${class##*.}"
			p1="${class%.$c1}"
			if test "$p" = "$p1"
			then
				c="$c, $c1"
			else
				test -z "$p" ||
				printf 'from %s import %s\n' "$p" "$c"
				p="$p1"
				c="$c1"
			fi
		done
		test -z "$p" ||
		printf 'from %s import %s\n' "$p" "$c")"
	;;
*.bsh)
	imports="$(echo "$toimport" |
		sed 's/.*/import &;/')"
	;;
*)
	die "Unhandled language: $path"
	;;
esac

test -z "$imports" ||
echo "$imports"
