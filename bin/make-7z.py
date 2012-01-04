#!/bin/sh
''''exec "$(dirname "$0")"/../ImageJ --jython "$0" "$@" # (call again with fiji)'''

import sys
from compat import execute

if len(sys.argv) < 3:
	print 'Usage: ', sys.argv[0], ' <archive> <folder>'
	exit(1)

archive = sys.argv[1]
folder = sys.argv[2]

print 'Making', archive, 'from', folder

if not archive.endswith('.7z'):
	archive = archive + '.7z'

execute('7z a -m0=lzma -mx=9 -md=64M ' + archive + ' ' + folder)
execute('chmod a+r ' + archive)
