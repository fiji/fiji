#!/bin/sh

git ls-remote --heads origin |
while read sha1 ref
do
	test $ref = refs/heads/master &&
	continue
	case "$(git merge-base $sha1 master)" in
	$sha1)
		echo origin/${ref#refs/heads/} is already in master;;
	esac
done
