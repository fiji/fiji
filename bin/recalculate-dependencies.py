#!/bin/sh
''''exec "$(dirname "$0")"/../fiji --jython "$0" "$@" # (call again with fiji)'''

from os import system
from sys import argv

from fiji.updater.logic import PluginCollection, XMLFileReader, XMLFileWriter
from java.io import FileInputStream, FileOutputStream
from java.lang.System import getProperty
from java.util.zip import GZIPInputStream, GZIPOutputStream

dbPath = getProperty('fiji.dir') + '/db.xml.gz'
plugins = PluginCollection()
XMLFileReader(plugins).read(None, GZIPInputStream(FileInputStream(dbPath)), 0)

for plugin in plugins:
	if plugin.current == None or not plugin.filename.endswith('.jar'):
		continue

	if len(argv) > 1:
		if not plugin.filename in argv[1:]:
			continue
		from ij import IJ
		IJ.debugMode = True

	print 'Handling', plugin
	dependencies = [dep.filename for dep in plugin.getDependencies()]
	seen = set()
	# dependencies have timestamps, so let's keep them intact when possible
	result = plugins.analyzeDependencies(plugin)
	for dependency in result:
		if dependency in dependencies:
			seen.add(dependency)
		else:
			plugin.addDependency(dependency)

	# special case: imglib dependency of Script Editor
	if plugin.filename == 'plugins/Script_Editor.jar':
		for dependency in ['jars/imglib.jar']:
			if not dependency in dependencies:
				plugin.addDependency(dependency)
				seen.add(dependency)

	for dependency in dependencies:
		if not dependency in seen:
			plugin.removeDependency(dependency)

writer = XMLFileWriter(plugins)
writer.validate()
writer.write(GZIPOutputStream(FileOutputStream(dbPath)))
