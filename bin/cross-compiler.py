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

def compile(prefix, platform, cflags, exe):
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

if platform == 'win64':
	compile('bin/win-sysroot/bin/x86_64-w64-mingw32-', platform, cflags, '.exe')
elif platform == 'win32':
	compile('bin/win-sysroot/bin/x86_64-w64-mingw32-', platform, cflags + ['-m32'], '.exe')
elif platform == 'linux' or platform == 'linux32':
	compile('', platform, cflags + ['-m32'], '')
elif platform == 'linux64':
	compile('', platform, cflags + ['-m64'], '')
elif platform == 'macosx' or platform.startswith('tiger') or platform == 'leopard':
	prefixppc = 'bin/mac-sysroot/bin/powerpc-apple-darwin8-'
	prefix32 = 'bin/mac-sysroot/bin/i686-apple-darwin8-'
	prefix64 = 'bin/mac-sysroot/bin/x86_64-apple-darwin8-'
	extra = ['-isysroot', 'bin/mac-sysroot', '-Ibin/mac-sysroot/usr/i686-apple-darwin8/usr/lib/gcc/i686-apple-darwin10/4.2.1/include/']
	if platform.startswith('tiger'):
		compile(prefixppc, 'tiger-ppc', cflags + extra + ['-m32'], '')
	if platform == 'macosx' or platform.startswith('tiger'):
		compile(prefix32, 'tiger-i686', cflags + extra + ['-m32'], '')
	if platform == 'macosx' or platform == 'leopard':
		compile(prefix64, 'leopard', cflags + extra + ['-m64'], '')
	if platform == 'tiger':
		command = prefix64 + 'lipo -create precompiled/fiji-tiger-ppc precompiled/fiji-tiger-i686 -output precompiled/fiji-tiger'
		print(command)
		print execute(command)
	if platform == 'macosx':
		command = prefix64 + 'lipo -create precompiled/fiji-tiger-i686 precompiled/fiji-leopard -output precompiled/fiji-macosx'
		print(command)
		print execute(command)
else:
	print "Unsupported platform:", sys.argv[1]
	sys.exit(1)


