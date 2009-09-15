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

if len(argv) != 2:
	print 'Need the location of a previous installation'
	exit(1)

tmpDirectory = None
if argv[1].endswith('.tar.bz2'):
	tmpDirectory = mktemp()
	mkdir(tmpDirectory)
	system('tar -C ' + tmpDirectory + ' --strip-components 1 -xvf ' \
		+ argv[1])
	argv[1] = tmpDirectory
elif argv[1].endswith('.zip'):
	tmpDirectory = mktemp()
	mkdir(tmpDirectory)
	system('unzip ' + argv[1] + ' -d ' + tmpDirectory)
	argv[1] = tmpDirectory + '/Fiji.app/'

dbPath = getProperty('fiji.dir') + '/db.xml.gz'
reader = XMLFileReader(GZIPInputStream(FileInputStream(dbPath)))

progress = StderrProgress()
checksummer = Checksummer(progress)
checksummer.updateFromPreviousInstallation(argv[1])
XMLFileWriter.writeAndValidate(dbPath[:-3])
system('gzip -9f ' + dbPath[:-3])

if tmpDirectory != None:
	def rmRecursively(file):
		if isdir(file):
			for f in listdir(file):
				rmRecursively(file + '/' + f)
			rmdir(file)
		else:
			remove(file)
	rmRecursively(tmpDirectory)
	print tmpDirectory
