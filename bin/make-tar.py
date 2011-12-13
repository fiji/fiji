#!/bin/sh
''''exec "$(dirname "$0")"/../ImageJ --jython "$0" "$@" # (call again with fiji)'''

import sys
from compat import execute

if len(sys.argv) < 3:
	print 'Usage: ', sys.argv[0], ' <tarfile> <folder>'
	exit(1)

tarfile = sys.argv[1]
folder = sys.argv[2]

print 'Making', tarfile, 'from', folder

if tarfile.endswith('.bz2'):
	packer = 'bzip2 -9 -f'
	tarfile = tarfile[:len(tarfile) - 4]
elif tarfile.endswith('.gz'):
	packer = 'gzip -9 -f'
	tarfile = tarfile[:len(tarfile) - 3]
else:
	packer = ''

execute('tar cvf ' + tarfile + ' ' + folder)

if packer != '':
	execute(packer + ' ' + tarfile)
