#!/bin/sh

cd "$(dirname "$0")"/..

EMAIL=fiji-devel@googlegroups.com
TMPFILE=.git/build.$$.out

(git fetch origin &&
 git reset --hard origin/master &&
 git clean -q -x -f &&
 find * -type d |
 while read dir
 do
	test ! -z "$(ls "$dir")" ||
	rm -r "$dir" ||
	break
 done &&
 ./Build.sh) > $TMPFILE 2>&1  &&
rm $TMPFILE || {
	mail -s "Fiji nightly build failed" \
		-a "Content-Type: text/plain; charset=UTF-8" \
		$EMAIL < $TMPFILE
	echo Failed: see $TMPFILE
}
