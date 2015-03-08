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
	sed 's/.*\///' |
	sort |
	uniq)"

printf "%s\n%s\n%s\n" "$candidates" "$ij1classes" |
	sort |
	uniq -d
