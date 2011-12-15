#!/bin/sh

case "$1" in
merged|unmerged)
	mode="$1"
	;;
*)
	echo "Usage: $0 (merged | unmerged)" >&2
	exit 1
	;;
esac

git ls-remote --heads origin |
while read sha1 ref
do
	ref=origin/${ref#refs/heads/}
	case $ref in
	origin/master|origin/debian-*)
		continue;; # ignore debian as a topic branch
	esac
	case "$(git merge-base $sha1 master)" in
	$sha1)
		case "$mode" in
		merged)
			test origin/contrib = "$ref" ||
			echo $ref is already in master;;
		esac;;
	*)
		case "$mode" in
		unmerged)
			date=$(git show -s --format='%cd' $ref)
			echo "origin/master..$ref ($date)":
			git log origin/master..$ref --format='    (%an) %s'
			echo;;
		esac;;
	esac
done |
less -FSRX
