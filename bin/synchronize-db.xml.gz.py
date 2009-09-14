#!/bin/sh
''''exec "$(dirname "$0")"/../fiji --jython "$0" "$@" # (call again with fiji)'''

from os import system
from sys import argv, exit

from fiji.updater.logic import Checksummer, PluginCollection, \
	XMLFileReader, XMLFileWriter
from fiji.updater.util import StderrProgress
from java.io import FileInputStream
from java.lang.System import getProperty
from java.util.zip import GZIPInputStream

if len(argv) != 2:
	print 'Need the location of a previous installation'
	exit(1)

dbPath = getProperty('fiji.dir') + '/db.xml.gz'
reader = XMLFileReader(GZIPInputStream(FileInputStream(dbPath)))

progress = StderrProgress()
checksummer = Checksummer(progress)
checksummer.updateFromPreviousInstallation(argv[1])
XMLFileWriter.writeAndValidate(dbPath[:-3])
system('gzip -9f ' + dbPath[:-3])
