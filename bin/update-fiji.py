#!/bin/sh
''''exec "$(dirname "$0")"/ImageJ.sh --jython "$0" "$@" # (call again with fiji)'''

from os import listdir, path
from sys import argv, exit, stderr

from fiji.updater import Updater
from fiji.updater.logic import Checksummer, FileUploader, \
	PluginCollection, PluginObject, PluginUploader, \
	XMLFileDownloader
from fiji.updater.logic.PluginObject import Action
from fiji.updater.util import StderrProgress, Util
from java.io import ByteArrayInputStream
from java.util import Calendar, Observer

if len(argv) > 1 and argv[1] == '--upload-to':
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
plugins = PluginCollection()

progress = StderrProgress()
checksummer = Checksummer(progress)
if len(files) == 0:
	checksummer.updateFromLocal()
else:
	if files[0] == '--auto':
		automatic = True
		files = files[1:]
	else:
		automatic = False
	checksummer.updateFromLocal(files)
	# check dependencies
	check = files
	needUpload = []
	while len(check) > 0:
		implied = []
		for file in check:
			plugin = plugins.getPlugin(file)
			dependencies = plugins.analyzeDependencies(plugin)
			if dependencies == None:
				continue
			for dependency in dependencies:
				if not dependency in files + needUpload:
					implied.append(dependency)
		if len(implied) == 0:
			break
		checksummer.updateFromLocal(implied)
		stillImplied = []
		for file in implied:
			plugin = plugins.getPlugin(file)
			if plugin.getStatus().isValid(Action.UPLOAD):
				stillImplied.append(file)
		needUpload.extend(stillImplied)
		check = stillImplied

	if len(needUpload) > 0 and not automatic:
		print
		print 'ERROR: These files would need to be uploaded, too:'
		print
		print ', '.join(needUpload)
		print
		print 'Run with --auto to make it so'
		print
		exit(1)

	plugins = [plugins.getPlugin(file) for file in files + needUpload]

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
