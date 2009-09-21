#!/bin/sh
''''exec "$(dirname "$0")"/../fiji --jython "$0" "$@" # (call again with fiji)'''

from fiji.updater.logic import Checksummer, PluginCollection
from fiji.updater.util import StderrProgress
from java.lang.System import getProperty

dbPath = getProperty('fiji.dir') + '/db.xml.gz'

progress = StderrProgress()
checksummer = Checksummer(progress)
checksummer.updateFromLocal()
plugins = PluginCollection.getInstance()
plugins.sort()
for plugin in plugins:
	print plugin, plugin.current.checksum
