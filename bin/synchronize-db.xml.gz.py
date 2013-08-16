#!/bin/sh
''''exec "$(dirname "$0")"/ImageJ.sh --jython "$0" "$@" # (call again with fiji)'''

from os import listdir, mkdir, remove, rmdir, system
from os.path import isdir
from re import compile
from sys import argv, exit
from sys.stderr import write
from tempfile import mktemp

from fiji.updater.logic import Checksummer, PluginCollection, \
	XMLFileReader, XMLFileWriter
from fiji.updater.util import StderrProgress, Util
from java.io import FileInputStream, FileOutputStream
from java.lang.System import getProperty
from java.util.zip import GZIPInputStream, GZIPOutputStream

dbPath = getProperty('ij.dir') + '/db.xml.gz'
plugins = PluginCollection()
XMLFileReader(plugins).read(None, GZIPInputStream(FileInputStream(dbPath)))

def addPreviousVersion(plugin, checksum, timestamp):
	p = plugins.getPlugin(plugin)
	if p != None:
		if not p.hasPreviousVersion(checksum):
			p.addPreviousVersion(checksum, timestamp)

prefix = '/var/www/update/'
pattern = compile('^(.*)-([0-9]{14})$')

def addPreviousVersions(path):
	write('Adding ' + path + '...\r')
	if isdir(prefix + path):
		names = listdir(prefix + path)
		names.sort()
		for name in names:
			if path != '':
				name = path + '/' + name
			addPreviousVersions(name)
	else:
		match = pattern.match(path)
		if match == None:
			return
		plugin = plugins.getPlugin(match.group(1))
		if plugin == None:
			print 'Ignoring', match.group(1)
			return
		checksum = Util.getDigest(match.group(1), prefix + path)
		timestamp = long(match.group(2))
		if not plugin.hasPreviousVersion(checksum):
			plugin.addPreviousVersion(checksum, timestamp)

addPreviousVersions('')

writer = XMLFileWriter(plugins)
writer.validate()
writer.write(GZIPOutputStream(FileOutputStream(dbPath)))
