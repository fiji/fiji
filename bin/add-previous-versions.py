#!/usr/bin/python

from fiji.pluginManager.utilities import PluginData
import os
import re
import sys

prefix = '/var/www/update/'
xmlPath = prefix + 'db.xml.gz'

pattern = re.compile('^(.*)-([0-9]{14})$')
updater = PluginData()

def addRecord(result, path):
	match = pattern.match(path)
	if match == None:
		return
	sys.stderr.write("\rAdding record for " + path + '... ')
	sha1 = updater.getDigest(match.group(1), prefix + path)
	size = os.stat(prefix + path).st_size
	if match.group(1) in result:
		result[match.group(1)][match.group(2)] = [sha1, size]
	else:
		result[match.group(1)] = dict({match.group(2): [sha1, size]})

def getDigests(result, dir):
	names = os.listdir(prefix + dir)
	names.sort()
	for name in names:
		if dir == '':
			path = name
		else:
			path = dir + '/' + name
		prefixed = prefix + path
		if os.path.isdir(prefixed):
			getDigests(result, path)
			continue
		addRecord(result, path)

def readXML(path):
	return ''.join(os.popen('gunzip < ' + path).readlines())

def writeXML(path, xml):
	f = os.popen('gzip -9 > ' + path, 'w')
	f.write(xml)
	f.close()

def insertRecord(xml, plugin, record):
	pluginTag = '<plugin filename="' + plugin + "\">\n"
	off = xml.find(pluginTag)
	if off < 0:
		off = xml.rfind('</plugin>')
		insert = "</plugin>\n    " + pluginTag
		xml = xml[:off] + insert + '    ' + xml[off:]
		off = off + len(insert)
		endOff = off
		versionBody = None
	else:
		off = off + len(pluginTag)
		versionOff = xml.find('<version ', off)
		bodyOff = xml.find(">\n", versionOff) + 2
		if xml[bodyOff - 3] == '/':
			versionBody = None
			bodyEndOff = bodyOff
		else:
			bodyEndOff = xml.find("        </version>\n", bodyOff)
			versionBody = xml[bodyOff:bodyEndOff]
		endOff = xml.find('    </plugin>', bodyEndOff)
	dates = record.keys()
	dates.sort()
	dates.reverse()
	insert = None
	for date in dates:
		metadata = record[date]
		if insert == None:
			insert = '        <version timestamp="' + date \
				+ '" checksum="' + metadata[0] \
				+ '" filesize="' + str(metadata[1]) + '\"'
			if versionBody == None:
				insert = insert + "/>\n"
			else:
				insert = insert + ">\n" + versionBody \
					 + "        </version>\n"
		else:
			insert = insert \
				+ '        <previous-version timestamp="' \
				+ date + '" checksum="' + metadata[0] + "\"/>\n"
	xml = xml[:off] + insert + xml[endOff:]
	return xml

xml = readXML(xmlPath)
digests = dict()
getDigests(digests, '')
for plugin in digests.keys():
	xml = insertRecord(xml, plugin, digests[plugin])
writeXML(xmlPath, xml)
