#!/bin/sh
''''exec "$(dirname "$0")"/../ImageJ --jython "$0" "$@" # (call again with fiji)'''

# Test whether any menu items contain pointers to non-existent classes which
# likely indicate a missconfiguration of a plugins.config file in a .jar plugin.

from ij import IJ, ImageJ, Menus

from java.lang import Class, ClassNotFoundException, NoClassDefFoundError

import sys

# Launch ImageJ
ImageJ()

if len(sys.argv) > 1 and sys.argv[1] == '-v':
	for key in Menus.getCommands():
		command = Menus.getCommands().get(key)
		print key, '->', command
	sys.exit()

ok = 1

def doesClassExist(name):
	try:
		IJ.getClassLoader().loadClass(name)
		return True
	except ClassNotFoundException:
		return False
	except NoClassDefFoundError:
		return False

# Inspect each menu command
for it in Menus.getCommands().entrySet().iterator():
	name = it.value
	paren = name.find('(')
	if -1 != paren:
		name = name[:paren]

	if not doesClassExist(name):
		# Try without the first package name, since it may be fake
		# for plugins in subfolders of the plugins directory:
		dot = name.find('.')
		if -1 == dot or not doesClassExist(name[dot+1:]):
			print 'ERROR: Class not found for menu command:', \
				it.key, '=>', it.value, \
				'in:', Menus.getJarFileForMenuEntry(it.key)
			ok = 0

if ok:
	print "ok - Menu commands all correct."
	sys.exit(0)

sys.exit(1)
