#!/bin/bash

cd "$(dirname $(readlink -nf $BASH_SOURCE))"/../src-plugins

for plugin in $(dpkg -L fiji-plugins | sed -n 's/.*\/\([^\/]*\)\.jar$/\1/p'| sort)
do
    echo $plugin
    git shortlog -n -s -- $plugin |
    sort |
    while read count author
    do
	years=$(git log --author="$author" --format=%ad -- $plugin |
	    cut -d \  -f 5 |
	    sort -u |
	    sed 's/$/,/' |
	    tr '\n' ' ')
	echo "   Copyright ${years%, } $author"
    done
    echo
done
