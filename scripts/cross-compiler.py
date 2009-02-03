#!/usr/bin/python

import os
import sys
import urllib
from compat import execute

if len(sys.argv) < 2:
	print "Usage:", sys.argv[0], "<platform> <cxxflags>"
	sys.exit(1)

if sys.argv[1] != 'win64':
	print "Unsupported platform:", sys.argv[1]
	sys.exit(1)

root = 'root-x86_64-pc-linux/'
cxx = root + 'bin/x86_64-pc-mingw32-g++'
strip = root + 'bin/x86_64-pc-mingw32-strip'
windres = root + 'bin/x86_64-pc-mingw32-windres'
target = 'precompiled/fiji-win64.exe'

if not os.path.exists(cxx):
	url = 'http://heanet.dl.sourceforge.net/sourceforge/mingw-w64/'
	file = 'mingw-w64-bin_x86-64-linux_20080721.tar.bz2'
	filename = urllib.urlretrieve(url + file)[0]
	if filename is None:
		print "You need to install the mingw64 cross compiler into", cxx
		sys.exit(1)
	os.makedirs(root)
	execute('tar -C ' + root + ' -xjvf ' + filename)
	if not os.path.exists(cxx):
		print "You need to install the mingw64 cross compiler into", cxx
		sys.exit(1)

res = open('tmp.rc', 'w')
res.write('101 ICON images/fiji.ico')
res.close()
print(windres + ' -o tmp.o -i tmp.rc')
print execute(windres + ' -o tmp.o -i tmp.rc')

quoted_args = ' '.join(sys.argv[2:]).replace("'", '"').replace('"', '\"')
print(cxx + ' -o ' + target + ' ' + quoted_args + ' fiji.cxx tmp.o')
print execute(cxx + ' -o ' + target + ' ' + quoted_args + ' fiji.cxx tmp.o')
print(strip + ' ' + target)
print execute(strip + ' ' + target)
