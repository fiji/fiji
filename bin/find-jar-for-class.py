#!/usr/bin/python

import sys
from java.lang import Class

for c in sys.argv[1:]:
	slashes = c.replace('.', '/')
	try:
		instance = Class.forName(c.replace('/', '.'))
		jar = instance.getResource('/' + slashes + '.class').toString()
		if jar.startswith('jar:file:'):
			jar = jar[9:]
		exclamation = jar.find('!/')
		if exclamation >= 0:
			jar = jar[0:exclamation]
		print 'Class', c, 'is in', jar
	except:
		print 'Class', c, 'was not found in the classpath'
