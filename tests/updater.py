#!/bin/sh
''''exec "$(dirname "$0")"/../fiji --jython "$0" "$@" # (call with fiji)'''

from lib import launchProgram, launchProgramNoWait
from os import listdir, makedirs, popen, remove, rename, rmdir
from os.path import dirname, exists, isdir
from shutil import copyfile
from sys import exit
from time import sleep

from java.lang import System

fijiDir = System.getProperty('fiji.dir') + '/'

tmpRoot = fijiDir + 'tests/tmpRoot/'
tmpWebRoot = fijiDir + 'tests/tmpWebRoot/'

def rmRF(file):
	if isdir(file):
		for f in listdir(file):
			rmRF(file + '/' + f)
		rmdir(file)
	else:
		remove(file)

for dir in [tmpRoot, tmpWebRoot]:
	if exists(dir):
		rmRF(dir)
	makedirs(dir)

# simulate empty webroot
f = popen('gzip -9 > ' + tmpWebRoot + 'db.xml.gz', 'w')
f.write('''<?xml version="1.0" encoding="UTF-8"?>
<pluginRecords/>
''')
f.close()

# populate webroot
uploadables = ['jars/ij.jar', 'jars/Fiji.jar', 'plugins/Fiji_Updater.jar',
	'jars/jsch-0.1.37.jar', 'jars/jzlib-1.0.7.jar',
	'plugins/Arrow_.jar', 'jars/javac.jar']
if launchProgram(['./fiji','-Dpython.cachedir.skip=true', '--',
		'--jython', 'bin/update-fiji.py',
		'--upload-to', tmpWebRoot] + uploadables, fijiDir) != 0:
	exit(1)

def die(message):
	print message
	exit(1)

# check that there are exactly the right number of files
extra = ['db.xml.gz', 'db.xml.gz.old', 'current.txt', 'misc', 'jars', 'plugins']
if len(uploadables) + len(extra) != sum([len(listdir(tmpWebRoot + dir)) for
		dir in ['.', 'misc', 'plugins', 'jars']]):
	die('Wrong number of files')

# populate with minimal Fiji; reuse Java
for file in ['fiji', 'jars/ij.jar', 'jars/Fiji.jar', 'plugins/Fiji_Updater.jar',
		'jars/jsch-0.1.37.jar', 'jars/jzlib-1.0.7.jar',
		'jars/javac.jar',
		'jars/fiji-scripting.jar', 'jars/jython.jar',
		'plugins/Jython_Interpreter.jar']:
	source = fijiDir + file
	target = tmpRoot + file
	dir = dirname(target)
	if not exists(dir):
		makedirs(dir)
	copyfile(source, target)

if launchProgram(['chmod', 'a+x', tmpRoot + 'fiji']) != 0:
	die('Could not make ' + tmpRoot + 'fiji executable')

# update some "packages"
f = open(tmpRoot + 'fiji.cxx', 'w')
f.write('pretend to be a developer')
f.close()

macros = tmpRoot + 'macros/'
if not exists(macros):
	makedirs(macros)

f = open(macros + 'updateable.ijm', 'w')
f.write('old')
f.close()

f = open(macros + 'deleted-modified.ijm', 'w')
f.write('this will be obsolete, but modified by the user')
f.close()

f = open(macros + 'obsolete.ijm', 'w')
f.write('this will be obsolete')
f.close()

uploadables = ['macros/updateable.ijm', 'macros/deleted-modified.ijm',
	    'macros/obsolete.ijm']
if launchProgram(['./fiji', '-Dpython.cachedir.skip=true', '--',
		'--jython', fijiDir + 'bin/update-fiji.py',
		'--upload-to', tmpWebRoot] + uploadables, tmpRoot) != 0:
	exit(1)

rename(macros + 'updateable.ijm', macros + 'outoftheway.ijm')
f = open(macros + 'updateable.ijm', 'w')
f.write('new')
f.close()

remove(macros + 'deleted-modified.ijm')

rename(macros + 'obsolete.ijm', macros + 'obsoleted.ijm')

# mark updateable.ijm with a platform
f = popen('gzip -d < ' + tmpWebRoot + 'db.xml.gz', 'r')
xml = ''.join(f.readlines())
f.close()

from re import DOTALL, compile, sub
from fiji.updater.util import Util
xml = sub('(</pluginRecords>)',
		'<plugin filename="thisPlatform">' \
		+ '<platform>' + Util.platform + '</platform>' \
		+ '<version checksum="bogus"/></plugin>' \
		+ '<plugin filename="otherPlatform">' \
		+ '<platform>bogus</platform>' \
		+ '<version checksum="bogus"/></plugin>\\1', xml)

f = popen('gzip -9 > ' + tmpWebRoot + 'db.xml.gz', 'w')
f.write(xml)
f.close()

if launchProgram(['./fiji', '-Dpython.cachedir.skip=true', '--',
		'--jython', fijiDir + 'bin/update-fiji.py',
		'--upload-to', tmpWebRoot] + uploadables, tmpRoot) != 0:
	exit(1)

f = open(tmpWebRoot + 'current.txt', 'r')
txt = ''.join(f.readlines())
f.close()

if txt.find('obsolete') >= 0:
	die('Obsolete files were added to current.txt')

remove(macros + 'updateable.ijm')
rename(macros + 'outoftheway.ijm', macros + 'updateable.ijm')

f = open(macros + 'deleted-modified.ijm', 'w')
f.write('modified by the user')
f.close()

rename(macros + 'obsoleted.ijm', macros + 'obsolete.ijm')

# pretend to be a user again
remove(tmpRoot + 'fiji.cxx')

# install a test script
action = tmpRoot + 'plugins/Test_Fiji_Updater.py'
script = open(action, 'w')
script.write('''
from java.lang import System

from fiji.updater import Updater

updater = Updater()
updater.MAIN_URL = 'file:''' + tmpWebRoot + ''''
updater.testRun = True
updater.run('update')

from fiji import Main

print 'waiting for frame'
updaterFrame = Main.waitForWindow('Fiji Updater')

from time import sleep
while updaterFrame.getLastModified() == 0:
	print 'Waiting for checksumming to finish'
	sleep(1)

from fiji.updater.logic.PluginObject import Action, Status

# test that the list is correct
expect = {
	'jars/Fiji.jar' : Status.INSTALLED,
	'plugins/Fiji_Updater.jar' : Status.INSTALLED,
	'jars/jsch-0.1.37.jar' : Status.INSTALLED,
	'jars/jzlib-1.0.7.jar' : Status.INSTALLED,
	'plugins/Arrow_.jar' : Status.NOT_INSTALLED,
	'macros/updateable.ijm' : Status.UPDATEABLE,
	'macros/deleted-modified.ijm' : Status.OBSOLETE_MODIFIED,
	'macros/obsolete.ijm' : Status.OBSOLETE,
	'jars/ij.jar' : Status.INSTALLED,
	'jars/javac.jar' : Status.INSTALLED,
	'jars/fiji-scripting.jar' : Status.NOT_FIJI,
	'jars/jython.jar' : Status.NOT_FIJI,
	'plugins/Jython_Interpreter.jar' : Status.NOT_FIJI,
	'plugins/Test_Fiji_Updater.py' : Status.NOT_FIJI,
	'thisPlatform' : Status.NOT_INSTALLED,
	'otherPlatform' : Status.NOT_INSTALLED
}

from fiji.updater.logic import PluginCollection
from java.lang.System import exit
plugins = PluginCollection.getInstance()
errorCount = 0
for plugin in plugins:
	if not expect.has_key(plugin.getFilename()):
		print 'Unexpected plugin:', plugin.getFilename()
		errorCount += 1
	status = expect[plugin.getFilename()]
	if status != plugin.getStatus():
		print 'Plugin', plugin.getFilename(), 'has unexpected status', \
			plugin.getStatus(), \
			'(expected:', status.toString() + ')'
		errorCount += 1

# test that the updateables are marked for update/uninstall
updateables = {
	'macros/updateable.ijm' : Action.UPDATE,
	'macros/obsolete.ijm' : Action.UNINSTALL
}
for plugin in plugins:
	action = plugin.getAction()
	if updateables.has_key(plugin.getFilename()):
		expected = updateables[plugin.getFilename()]
	else:
		expected = plugin.getStatus().getNoAction()
	if action != expected:
		print plugin.getFilename(), 'has action', action, 'but', \
			expected, 'was expected'
		errorCount += 1

# test that the updateables are shown
shownByDefault = PluginCollection.clone(plugins.shownByDefault())
for plugin in updateables:
	if shownByDefault.getPlugin(plugin) == None:
		print 'Updateable', plugin, 'not shown'
		errorCount += 1

# start update
updaterFrame.applyChanges()

# test that the updateables are in update/
from os.path import exists
for plugin in updateables:
	if not exists('update/' + plugin):
		print plugin, 'is not in update/'
		errorCount += 1

# test that the updated updateables are no longer shown
shownByDefault = PluginCollection.clone(plugins.shownByDefault())
for plugin in updateables:
	if shownByDefault.getPlugin(plugin) != None:
		print 'Updateable', plugin, 'still shown'
		errorCount += 1

# test that platform was preserved
for plugin in ['thisPlatform', 'otherPlatform']:
	if sum([1 for p in plugins.getPlugin(plugin).getPlatforms()]) == 0:
		print 'Platform not preserved for', plugin
		errorCount += 1

# verify that only those with the correct platform are shown
notHidden = PluginCollection.clone(plugins.notHidden())
if notHidden.getPlugin('thisPlatform') == None:
	print 'File with correct platform not shown!'
	errorCount += 1
if notHidden.getPlugin('otherPlatform') != None:
	print 'File with wrong platform not hidden!'
	errorCount += 1

# verify that the uninstalled obsolete plugin is no longer shown
if notHidden.getPlugin('macros/obsolete.ijm') != None:
	print 'Obsolete file which was uninstalled is not hidden!'
	errorCount += 1
if errorCount > 0:
	print 'The plugin list is:'
	for plugin in plugins:
		print plugin.getFilename() \
			+ ', status ' + plugin.getStatus().toString() \
			+ ', action ' + plugin.getAction().toString()
	exit(1)

print 'Everything fine!'
#exit(0)
''')
script.close()

# launch
if launchProgram(['./fiji', '-Dpython.cachedir.skip=true',
	'--run', 'Test_Fiji_Updater'], tmpRoot) != 0:
	die('The Fiji Updater test failed')
