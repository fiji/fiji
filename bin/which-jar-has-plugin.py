#!/bin/sh
''''exec "$(dirname "$0")"/../ImageJ --jython "$0" "$@" # (call again with fiji)'''


from java.io import File

from java.lang import System

import sys

from zipfile import ZipFile

if len(sys.argv) < 2:
	print 'Usage:', sys.argv[0], '<name>...'
	sys.exit(1)

jars = System.getProperty("java.class.path").split(File.pathSeparator)

for name in sys.argv[1:]:
	key = '"' + name + '"'
	for jar in jars:
		try:
			config = ZipFile(jar, 'r').read('plugins.config')
			if config.find(key) > 0:
				print jar, 'contains', key
		except:
			pass # do nothing
