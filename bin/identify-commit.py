#!/bin/sh
''''exec "$(dirname "$0")"/../fiji --jython "$0" "$@" # (call again with fiji)'''

from java.lang.System import getProperty

# Find out the checksum

dbPath = getProperty('fiji.dir') + '/db.xml.gz'

def getTimestamp(plugin, checksum):
	from fiji.updater.logic import PluginCollection, XMLFileReader
	if PluginCollection.getInstance().size() == 0:
		from java.io import FileInputStream
		from java.util.zip import GZIPInputStream
		print 'Reading db.xml.gz'
		XMLFileReader(GZIPInputStream(FileInputStream(dbPath)), 0)
	if plugin.startswith('precompiled/'):
		plugin = plugin[12:]
	plugin = PluginCollection.getInstance().getPlugin(plugin)
	if plugin.current != None and checksum == plugin.current.checksum:
		return plugin.current.timestamp
	for version in plugin.previous.keySet():
		if checksum == version.checksum:
			return version.timestamp

from sys import argv

if len(argv) == 2:
	from fiji.updater.util import Util
	print 'Checksumming', argv[1]
	checksum = Util.getDigest(argv[1], argv[1])
	timestamp = getTimestamp(argv[1], checksum)
	print 'Got checksum', checksum
	print '\t... which corresponds to timestamp', timestamp
elif len(argv) == 3:
	if len(argv[2]) == 40:
		timestamp = getTimestamp(argv[1], argv[2])
	else:
		timestamp = argv[2]
	print 'Timestamp', timestamp, 'for', argv[1]
else:
	from sys import exit
	print 'Usage:', argv[0], '<jar>', '[<timestamp-or-checksum>]'
	exit(1)

if timestamp == None:
	print 'No timestamp found for', argv[1], '(locally modified)'
	from sys import exit
	exit(1)

# Find out which path the jar comes from

from fiji.build import Fake

fakefile = getProperty('fiji.dir') + '/Fakefile'

fake = Fake()
from java.io import File, FileInputStream
parser = fake.parse(FileInputStream(fakefile), File(getProperty('fiji.dir')))
parser.parseRules([])
print 'Getting rule for', argv[1]
rule = parser.getRule(argv[1])
prereq = rule.getPrerequisiteString()
space = prereq.find(' ')
if space < 0:
	space = len(prereq)
star = prereq.find('*', 0, space)
if star < 0:
	star = space
slash = prereq.rfind('/', 0, star)
if slash > 0:
	prereq = prereq[:slash]

timestamp = str(timestamp)
from datetime import datetime, timedelta
date = datetime.strptime(timestamp[6:8] + '.' + timestamp[4:6] + '.' + timestamp[:4] + ' ' + timestamp[8:10] + ':' + timestamp[10:12] + ':' + timestamp[12:14] + ' +0000', '%d.%m.%Y %H:%M:%S')
startdate = date - timedelta(0, 2 * 60 * 60)
enddate = date + timedelta(0, 2 * 60 * 60)
date = date.strftime('%d.%m.%Y.%H:%M:%S')
startdate = startdate.strftime('%d.%m.%Y.%H:%M:%S')
enddate = enddate.strftime('%d.%m.%Y.%H:%M:%S')

from compat import execute
cmd = 'git log --since=' + startdate + ' --until=' + enddate + ' ' + prereq
print 'Running:', cmd
result = execute(cmd)
if result == '':
	cmd = 'git log -1 --until=' + startdate + ' ' + prereq
	print 'Running again:', cmd
	result = execute(cmd)

print '\nFound:'
print result
