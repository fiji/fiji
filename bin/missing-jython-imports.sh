#!/bin/sh

path="$1"

candidates="$(cat "$path" |
	grep -v ^from |
	tr ' 	,().;:' '\n' |
	grep '^[A-Z]' |
	grep -v '"' |
	sort |
	uniq)"

imported="$(cat "$path" |
	sed -n 's/^from .* import //p' |
	tr ' ,' '\n' |
	grep -v '^$' |
	sort |
	uniq)"

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
	eval grep $(echo "$missing" | sed 's/.*/-e \\.&$/'))"

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

test -z "$imports" ||
echo "$imports"
