#!/bin/sh
''''exec "$(dirname "$0")"/../fiji --jython "$0" "$@" # (call again with fiji)'''

import os
import sys
import urllib
from compat import execute

if len(sys.argv) < 2:
	print "Usage:", sys.argv[0], "<platform> <cflags>"
	sys.exit(1)

# avoid errors due to sys.stdout.encoding == 'ascii'
from org.python.core import codecs
codecs.setDefaultEncoding('utf-8')

platform = sys.argv[1]
cflags = sys.argv[2:]

prefix = ''
exe = ''
if platform == 'win64':
	prefix = 'bin/win-sysroot/bin/x86_64-w64-mingw32-'
	exe = '.exe'
elif platform == 'win32':
	prefix = 'bin/win-sysroot/bin/x86_64-w64-mingw32-'
	cflags.append('-m32')
	exe = '.exe'
elif platform == 'linux' or platform == 'linux32':
	cflags.append('-m32')
else:
	print "Unsupported platform:", sys.argv[1]
	sys.exit(1)

cc = prefix + 'gcc'
strip = prefix + 'strip'
windres = prefix + 'windres'
target = 'precompiled/fiji-' + platform + exe

source = 'fiji.c'
if platform.startswith('win'):
	res = open('tmp.rc', 'w')
	res.write('101 ICON images/fiji.ico')
	res.close()
	if platform == 'win32':
		m32 = '--target=pe-i386'
	else:
		m32 = ''
	command = windres + ' ' + m32 + ' -o tmp.o -i tmp.rc'
	print(command)
	print execute(command)
	source += " tmp.o"

quoted_args = ' '.join(cflags).replace("'", '"').replace('"', '\"')
command = cc + ' -o ' + target + ' ' + quoted_args + ' ' + source
print(command)
print execute(command)
command = strip + ' ' + target
print(command)
print execute(command)
