#!/bin/sh
''''exec "$(dirname "$0")"/../fiji --jython "$0" "$@" # (call with fiji)'''

from fiji.updater.logic import PluginCollection

plugins = PluginCollection.getInstance()
plugins.removeAll(plugins)

# create a few fake entries
from fiji.updater.logic import Dependency
from fiji.updater.logic import PluginObject
from fiji.updater.logic.PluginObject import Action
from fiji.updater.logic.PluginObject import Status

def addPlugin(filename, checksum, status, dependencies):
	plugin = PluginObject(filename, checksum, 0, status)
	for dependency in dependencies:
		plugin.addDependency(dependency)
	plugins.add(plugin)
	return plugin

big = addPlugin('big-plugin', 'trakem', Status.UPDATEABLE,
	['small-plugin', 'updateable'])
small = addPlugin('small-plugin', 'mpicbg', Status.MODIFIED,
	['library', 'obsoleted'])
library = addPlugin('library', None, Status.NOT_INSTALLED, [])
updateable = addPlugin('updateable', 'old', Status.UPDATEABLE,
	['library2'])
updateable2 = addPlugin('updateable2', 'old', Status.UPDATEABLE,
	['library2'])
library2 = addPlugin('library2', None, Status.NOT_INSTALLED, [])
obsoleted = addPlugin('obsoleted', 'stillthere', Status.OBSOLETE, [])

big.setAction(Action.UPDATE)

from fiji.updater.ui import ResolveDependencies

dialog = ResolveDependencies(None)
dialog.setModal(False) # for debugging
dialog.resolve()

errorCount = 0

ok = dialog.getContentPane().getComponents()[1].getComponents()[0]
if ok.getLabel() != 'OK':
	print 'Okay button has label', ok.getLabel()

expected = ['Keep the local version', 'Update small-plugin']
i = 0
for comp in dialog.panel.getComponents():
	# the components in the JTextPane are wrappers around the buttons
	for subcomp in comp.getComponents():
		if subcomp.getLabel() != expected[i]:
			print 'Label ', i, 'is', subcomp.getLabel(), \
				'(expected', expected[i] + ')'
			errorCount += 1
		i += 1

# trigger "Keep local version"
dialog.panel.getComponents()[0].getComponents()[0].doClick();

if not ok.isEnabled():
	print 'Resolution did not work?'
	errorCount += 1

expected = dict({
	big : Action.UPDATE,
	small : Action.MODIFIED,
	library : Action.NOT_INSTALLED,
	updateable : Action.UPDATEABLE,
	updateable2 : Action.UPDATEABLE,
	library2 : Action.NOT_INSTALLED,
	obsoleted : Action.OBSOLETE
})

for plugin in expected:
	if plugin.getAction() != expected[plugin]:
		print 'Plugin', plugin.toDebug(), 'expected:', expected[plugin]
		errorCount += 1

ok.doClick()

expected = dict({
	big : Action.UPDATE,
	small : Action.MODIFIED,
	library : Action.INSTALL,
	updateable : Action.UPDATE,
	updateable2 : Action.UPDATEABLE,
	library2 : Action.INSTALL,
	obsoleted : Action.UNINSTALL
})

for plugin in expected:
	if plugin.getAction() != expected[plugin]:
		print 'Plugin', plugin.toDebug(), 'expected:', expected[plugin]
		errorCount += 1

from sys import exit
if errorCount > 0:
	exit(1)
