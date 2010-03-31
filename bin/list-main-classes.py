#!/bin/sh
''''exec "$(dirname "$0")"/../fiji --jython "$0" "$@" # (call again with fiji)'''

from fiji import FijiClassLoader

from jarray import array, zeros

from java.lang import Class, Object, String

import sys

from zipfile import ZipFile

if len(sys.argv) < 2:
	print 'Usage:', sys.argv[0], '( --all | <jar>...)'
	sys.exit(1)

if sys.argv[1] == '--all':
	jars = System.getProperty("java.class.path").split(File.pathSeparator)
else:
	jars = sys.argv[1:]

classLoader = FijiClassLoader()
args = array([Object.getClass(zeros(0, String))], Class)
def hasMainMethod(name):
	try:
		c = Class.forName(name, False, classLoader)
		return c.getMethod('main', args) != None
	except:
		return False

for jar in jars:
	try:
		classLoader.addPath(jar)
		zip = ZipFile(jar, 'r')
		for file in zip.namelist():
			if not file.endswith('.class'):
				continue
			name = file[:-6].replace('/', '.')
			if hasMainMethod(name):
				print 'main class', name, 'found in jar', jar
	except:
		pass # do nothing
