#!/usr/bin/python

import UpdateFiji
import os
import re

prefix = '/var/www/update/'
xml = 'db.xml.gz'

pattern = re.compile('^(.*)-([0-9]{14})$')
updater = UpdateFiji()

def getDigests(dir):
	result = []
	names = os.listdir(prefix + dir)
	for name in names:
		if dir == '':
			prefixed = name
		else:
			prefixed = dir + '/' + name
		path = prefix + prefixed
		if os.path.isdir(path):
			result.extend(getDigests(prefixed))
			continue
		match = pattern.match(prefixed)
		if match == None:
			continue
		sha1 = updater.getDigest(match.group(1), path)
		result.append([match.group(1), match.group(2), sha1])

	return result

def getXML(path):
	return ''.join(os.popen('gunzip < ' + path).readlines())

def insertRecord(xml, record):
	if xml.find(record[2]) >= 0:
		return xml # already in there
	off = xml.find('<plugin filename="' + record[0] + '">')
	previous = "\n        <previous-version timestamp=\"" \
		+ record[1] + '" checksum="' + record[2] + '"/>'
	if off < 0:
		off = xml.rfind('</plugin>')
		xml = xml[:off] + "</plugin>\n    <plugin filename=\"" \
			+ record[0] + "\">" + previous + "\n    " + xml[off:]
		return xml
	endoff = xml.find('</plugin>', off)
	eol = xml.rfind("\n", 0, endoff)
	return xml[:eol] + previous + xml[eol:]

xml = getXML(xml)
for record in getDigests(''):
	xml = insertRecord(xml, record)
print xml
