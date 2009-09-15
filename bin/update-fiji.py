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

Updater.MAIN_URL = 'file:' + updateDirectory
downloader = XMLFileDownloader()
downloader.start()
reader = XMLFileReader(downloader.getInputStream())

progress = StderrProgress()
checksummer = Checksummer(progress)
if len(argv) == 1:
	checksummer.updateFromLocal()
else:
	checksummer.updateFromLocal(argv[1:])

# mark as update
collection = PluginCollection.getInstance()
now = Util.timestamp(Calendar.getInstance())
if len(argv) == 1:
	plugins = collection
else:
	plugins = [collection.getPlugin(file) for file in argv[1:]]
	# TODO: add dependencies
for plugin in plugins:
	for action in [Action.UPLOAD, Action.REMOVE]:
		if plugin.getStatus().isValid(action):
			plugin.setAction(action)
			break
	if plugin.getAction() == plugin.getStatus().getNoAction():
		print 'Leaving', plugin.getFilename(), 'alone:', \
			plugin.getStatus(), plugin.getAction()


uploader = PluginUploader(downloader.getXMLLastModified())
uploader.setUploader(FileUploader(updateDirectory))
uploader.upload(progress)
