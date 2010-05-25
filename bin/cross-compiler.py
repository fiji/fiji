#!/bin/sh
''''exec "$(dirname "$0")"/../fiji --jython "$0" "$@" # (call again with fiji)'''

import os
import sys
import urllib
from compat import execute

if len(sys.argv) < 2:
	print "Usage:", sys.argv[0], "<platform> <cflags>"
	sys.exit(1)

if sys.argv[1] != 'win64':
	print "Unsupported platform:", sys.argv[1]
	sys.exit(1)

root = 'root-x86_64-pc-linux/'
cc = root + 'bin/x86_64-w64-mingw32-gcc'
strip = root + 'bin/x86_64-w64-mingw32-strip'
windres = root + 'bin/x86_64-w64-mingw32-windres'
target = 'precompiled/fiji-win64.exe'

if not os.path.exists(cc):
	url = 'http://dfn.dl.sourceforge.net/project/mingw-w64/'
	url += 'Toolchains%20targetting%20Win64/Automated%20Builds/'
	file = 'mingw-w64-bin_x86_64-linux_20090920.tar.bz2'
	execute('curl -o ' + file + ' ' + url + file )
	filename = file
	if filename is None:
		print "You need to install the mingw64 cross compiler into", cc
		sys.exit(1)
	os.makedirs(root)
	execute('tar -C ' + root + ' -xjvf ' + filename)
	if not os.path.exists(cc):
		print "You need to install the mingw64 cross compiler into", cc
		sys.exit(1)

res = open('tmp.rc', 'w')
res.write('101 ICON images/fiji.ico')
res.close()
print(windres + ' -o tmp.o -i tmp.rc')
print execute(windres + ' -o tmp.o -i tmp.rc')

quoted_args = ' '.join(sys.argv[2:]).replace("'", '"').replace('"', '\"')
print(cc + ' -o ' + target + ' ' + quoted_args + ' -static fiji.c tmp.o')
print execute(cc + ' -o ' + target + ' ' + quoted_args + ' -static fiji.c tmp.o')
print(strip + ' ' + target)
print execute(strip + ' ' + target)
