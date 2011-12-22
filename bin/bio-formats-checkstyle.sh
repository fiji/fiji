#!/bin/sh

cd "$(dirname "$0")"/../bio-formats &&
test -d .git || {
	echo "Bio-Formats was not checked out" >&2
	exit 1
}

files="$(git diff --name-only HEAD)"
test -z "$files" && files = "$(git diff --name-only HEAD^)"

../ImageJ --jar jar/checkstyle-all-4.2.jar --jarpath jar \
		-c checkstyle.xml $files
