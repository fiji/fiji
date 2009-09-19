#!/bin/sh
''''exec "$(dirname "$0")"/../fiji --jython "$0" "$@" # (call again with fiji)'''

from os import listdir, mkdir, remove, rmdir, system
from os.path import isdir
from sys import argv, exit
from tempfile import mktemp

from fiji.updater.logic import Checksummer, PluginCollection, \
	XMLFileReader, XMLFileWriter
from fiji.updater.util import StderrProgress
from java.io import FileInputStream
from java.lang.System import getProperty
from java.util.zip import GZIPInputStream

dbPath = getProperty('fiji.dir') + '/db.xml.gz'
XMLFileReader(GZIPInputStream(FileInputStream(dbPath)))

progress = StderrProgress()
#checksummer = Checksummer(progress)
#checksummer.updateFromLocal()
plugins = PluginCollection.getInstance()
for plugin in plugins:
	plugins.updateDependencies(plugin)
	print plugin.getFilename() + ':', ', '.join([dependency.filename \
		for dependency in plugin.getDependencies()])
XMLFileWriter.writeAndValidate(dbPath[:-3])
system('gzip -9f ' + dbPath[:-3])
