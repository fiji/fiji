#!/bin/sh

test $# = 0 && {
	echo "Usage: $0 <package>"
	exit 1
}

"$(dirname "$0")"/../fiji --jython /dev/stdin << EOF
import $1

for item in dir($1):
	print item
EOF
