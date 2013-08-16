#!/bin/sh
''''exec "$(dirname "$0")"/../ImageJ --jython "$0" "$@" # (call again with fiji)'''

# Test that ImageJ(A) can add to any menu, and that the result is
# appropriately separated by a separator, and sorted alphabetically.

from ij import IJ, ImageJ, Menus, Prefs, WindowManager

from java.awt import Menu, MenuBar
from java.lang import System
from java.io import FileOutputStream
from java.util.zip import ZipOutputStream, ZipEntry

import os

# make a temporary directory, and put two fake .jar files in it

# Warning: Jython does not support removedirs() yet
def removedirs(dir):
	os.system('rm -rf ' + dir)

temporary_folder = 'tests/plugins'
removedirs(temporary_folder)
os.makedirs(temporary_folder)

def fake_plugin_jar(name, plugins_config):
	output = FileOutputStream(name)
	zip = ZipOutputStream(output)
	entry = ZipEntry('plugins.config')
	zip.putNextEntry(entry)
	zip.write(plugins_config)
	zip.closeEntry()
	zip.close()

def fake_plugin_class(name):
	slash = name.rfind('/')
	if slash > 0 and not os.path.isdir(name[:slash]):
		os.makedirs(name[:slash])
	f = open(name + '.class', 'w')
	f.write('Invalid class')
	f.close()

def update_menus():
	try:
		IJ.redirectErrorMessages()
		IJ.run('Update Menus')
	except:
		error_message = 'Error updating menus'
		logWindow = WindowManager.getFrame("Log")
		if not logWindow is None:
			error_message = error_message + ': ' \
				+ logWindow.getTextPanel().getText()
			logWindow.close()
		print error_message
		global error
		error += 1

fake_plugin_jar(temporary_folder + '/test_.jar',
	'Image>Color>Hello, "Cello", Cello')
fake_plugin_jar(temporary_folder + '/test_2.jar',
	'Image>Color>Hello, "Bello", Bello' + "\n" +
	'Image>Color>Hello, "Allo", Bello' + "\n" +
	'Plugins>bla>blub>, "Eldo", Rado' + "\n" +
	'Plugins>bla>blub>, "Bubble", Rado' + "\n" +
	'Plugins, "Cello", xyz' + "\n" +
	'Plugins, "Abracadabra", abc')

# reset the plugins folder to the temporary directory

System.setProperty('plugins.dir', temporary_folder);

# Launch ImageJ

IJ.redirectErrorMessages()
ij = ImageJ()
error = 0

# Must show Duplicate command error

logWindow = WindowManager.getFrame("Log")
if logWindow is None:
	print 'No error adding duplicate entries'
	error += 1
else:
	logText = logWindow.getTextPanel().getText()
	if not 'Duplicate command' in logText:
		print 'Error adding duplicate entries, but the wrong one'
		error += 1
	logWindow.close()

# debug functions

def printMenu(menu, indent):
	n = menu.getItemCount()
	for i in range(0, n):
		item = menu.getItem(i)
		print indent, item.getLabel()
		if isinstance(item, Menu):
			printMenu(item, indent + '    ')

def printAllMenus():
	mbar = Menus.getMenuBar()
	n = mbar.getMenuCount()
	for i in range(0, n):
		menu = mbar.getMenu(i)
		print menu.getLabel()
		printMenu(menu, '    ')
	print 'Help'
	printMenu(mbar.getHelpMenu(), '    ')

# make sure that something was inserted into Image>Color

def getMenuEntry(path):
	if isinstance(path, str):
		path = path.split('>')
	try:
		menu = None
		mbar = Menus.getMenuBar()
		for i in range(0, mbar.getMenuCount()):
			if path[0] == mbar.getMenu(i).getLabel():
				menu = mbar.getMenu(i)
				break
		for j in range(1, len(path)):
			entry = None
			for i in range(0, menu.getItemCount()):
				if path[j] == menu.getItem(i).getLabel():
					entry = menu.getItem(i)
					break
			menu = entry
		return menu
	except:
		return None

if getMenuEntry('Image>Color>Hello>Bello') is None:
	print 'Bello was not inserted at all'
	error += 1

# make sure that added submenus are sorted

def isSorted(path, onlyAfterSeparator):
	menu = getMenuEntry(path)
	if menu is None:
		return False
	for i in range(0, menu.getItemCount() - 1):
		if onlyAfterSeparator:
			if menu.getItem(i).getLabel() == '-':
				onlyAfterSeparator = False
			continue
		if menu.getItem(i).getLabel() > menu.getItem(i + 1).getLabel():
			return False
	return True

if isSorted('Image>Color>Hello', False):
	print 'Image>Color>Hello was sorted'
	error += 1

if not isSorted('Plugins', True):
	print 'Plugins was not sorted'
	error += 1

if not isSorted('Plugins>bla>blub', True):
	print 'Plugins>bla>blub was not sorted'
	error += 1

os.remove(temporary_folder + '/test_2.jar')
fake_plugin_jar(temporary_folder + '/test_3.jar',
	'Image>Color>Hello, "Zuerich", Zuerich')

update_menus()

if not getMenuEntry('Image>Color>Hello>Bello') is None:
	print 'Update Menus kept Bello'
	error += 1

if getMenuEntry('Image>Color>Hello>Zuerich') is None:
	print 'Update Menus did not insert Zuerich'
	error += 1

# Test isolated classes

fake_plugin_class(temporary_folder + '/Some_Isolated_Class')
fake_plugin_class(temporary_folder + '/Another/Isolated_Class')
Prefs.moveToMisc = True

update_menus()

if getMenuEntry('Plugins>Some Isolated Class') is None:
	print 'Isolated class not put into toplevel'
	error += 1

if not getMenuEntry('Plugins>Another>Isolated Class') is None:
	print 'Isolated class in subdirectory put into toplevel'
	error += 1

if getMenuEntry('Plugins>Miscellaneous>Isolated Class') is None:
	print 'Isolated class in subdirectory not put into misc menu'
	error += 1

# Test that 'Quit' is always last item in the File menu

fake_plugin_jar(temporary_folder + '/test_4.jar',
	'File, "Something", Wuerzburg')

update_menus()

if getMenuEntry('File>Something') is None:
	print 'File>Something is missing'
	error += 1

file = getMenuEntry('File')
if file is None:
	print 'Huh? File menu is missing!'
	error += 1
elif file.getItem(file.getItemCount() - 1).getLabel() != 'Quit':
	print 'Last item in File menu is not Quit!'
	error += 1

ij.exitWhenQuitting(True)
ij.quit()
