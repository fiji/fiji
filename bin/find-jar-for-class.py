#!/bin/sh
''''exec "$(dirname "$0")"/../ImageJ --jython "$0" "$@" # (call again with fiji)'''

import sys
from java.lang import ClassLoader

classLoader = ClassLoader.getSystemClassLoader()
for c in sys.argv[1:]:
	slashed = c.replace('.', '/') + '.class'
	try:
		jar = classLoader.getResource(slashed).toString()
		if jar.startswith('jar:file:'):
			jar = jar[9:]
		exclamation = jar.find('!/')
		if exclamation >= 0:
			jar = jar[0:exclamation]
		print 'Class', c, 'is in', jar
	except:
		print 'Class', c, 'was not found in the classpath'
