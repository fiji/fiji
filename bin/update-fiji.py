#!/bin/sh
''''exec "$(dirname "$0")"/../fiji --jython "$0" "$@" # (call again with fiji)'''

from os import listdir, path
from sys import argv, stderr

from fiji.updater import Updater
from fiji.updater.logic import Checksummer, FileUploader, \
	PluginCollection, PluginObject, PluginUploader, \
	XMLFileDownloader, XMLFileReader
from fiji.updater.logic.PluginObject import Action
from fiji.updater.util import StderrProgress, Util
from java.io import ByteArrayInputStream
from java.util import Calendar, Observer

if argv[1] == '--upload-to':
	updateDirectory = argv[2]
	argv[1:3] = []
else:
	updateDirectory = '/var/www/update/'

def stripPrecompiled(string):
	if string.startswith('precompiled/'):
		return string[12:]
	return string

files = [stripPrecompiled(file) for file in argv[1:]]

Updater.MAIN_URL = 'file:' + updateDirectory
downloader = XMLFileDownloader()
downloader.start()
reader = XMLFileReader(downloader.getInputStream())

progress = StderrProgress()
checksummer = Checksummer(progress)
plugins = PluginCollection.getInstance()
if len(files) == 0:
	checksummer.updateFromLocal()
else:
	checksummer.updateFromLocal(files)
	plugins = [plugins.getPlugin(file) for file in files]
	# TODO: add dependencies

# mark for update
def markForUpdate(plugin):
	for action in [Action.UPLOAD, Action.REMOVE]:
		if plugin.getStatus().isValid(action):
			plugin.setAction(action)
			return True
	return False

for plugin in plugins:
	if not markForUpdate(plugin):
		print 'Leaving', plugin.getFilename(), 'alone:', \
			plugin.getStatus(), plugin.getAction()


uploader = PluginUploader(downloader.getXMLLastModified())
uploader.setUploader(FileUploader(updateDirectory))
uploader.upload(progress)
