#!/bin/sh
''''exec "$(dirname "$0")"/ImageJ.sh --jython "$0" "$@" # (call again with fiji)'''

# Fetch all the samples into a local directory

from fiji import User_Plugins

from ij import IJ, Menus

from java.lang import System, Thread

from os import makedirs, rename, stat

from os.path import exists, isdir

from sys import stderr

from time import sleep

import urllib

# force initialization
IJ.runMacro("", "")

menu = FijiTools.getMenuItem('File>Open Samples')
commands = Menus.getCommands()
plugin = 'ij.plugin.URLOpener("'
samples = System.getProperty('ij.dir') + '/samples'

class FileSizeReporter(Thread):
	def __init__(self, name, path):
		self.name = name
		self.path = path
		self.canceled = False

	def run(self):
		while self.canceled == False:
			if exists(self.path):
				stderr.write('\rDownload ' + self.name \
					+ ': ' + str(stat(self.path).st_size) \
					+ ' bytes')
			sleep(1)

urls = []
if menu != None:
	for i in range(0, menu.getItemCount()):
		label = menu.getItem(i).getLabel()
		if label == '-':
			continue
		command = commands[label]
		if command != None and \
				command.startswith(plugin) and command.endswith('")'):
			url = command[len(plugin):-2]
			urls.append(url)
		else:
			print 'Skipping unknown command', command, 'for label', label
else:
	for label in commands:
		command = commands[label]
		if command != None and \
				command.startswith(plugin) and command.endswith('")'):
			url = command[len(plugin):-2]
			urls.append(url)

for url in urls:
	slash = url.rfind('/')
	if slash < 0:
		name = url
		url = IJ.URL + '/images/' + url
	else:
		name = url[slash + 1:]
	target = samples + '/' + name
	if exists(target):
		print 'Already have', name
		continue

	reporter = FileSizeReporter(name, target)
	reporter.start()

	stderr.write('Download ' + name)
	if not isdir(samples):
		makedirs(samples)
	filename = urllib.urlretrieve(url, target)[0]
	if filename != target:
		rename(filename, target)

	reporter.canceled = True
	stderr.write('\rDownloaded ' + name + '                 \n')

print 'Done'
