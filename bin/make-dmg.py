#!/usr/bin/python

import os
import re

from compat import symlink, execute

dmg='fiji-macosx.dmg'
app='Fiji.app'

def hdiutil(cmd):
	print cmd
	os.system('hdiutil ' + cmd)
def get_disk_id(dmg):
	match=re.match('.*/dev/([^ ]*)[^/]*Apple_HFS.*', execute('hdid ' + dmg),
		re.MULTILINE | re.DOTALL)
	if match != None:
		return match.group(1)
	return None
def get_folder(dmg):
	match=re.match('.*Apple_HFS\s*([^\n]*).*', execute('hdid ' + dmg),
		re.MULTILINE | re.DOTALL)
	if match != None:
		return match.group(1)
	return None
def eject(dmg):
	disk_id=get_disk_id(dmg)
	print "disk_id: ", disk_id
	hdiutil('eject ' + disk_id)

# create temporary disk image and format, ejecting when done
hdiutil('create ' + dmg + ' -srcfolder ' + app \
	+ ' -fs HFS+ -format UDRW -volname Fiji -ov')
folder=get_folder(dmg)
print "folder: ", folder
for d in ['plugins', 'macros']:
	symlink('Fiji.app/' + d, folder + '/' + d)
eject(dmg)

os.rename(dmg, dmg + '.tmp')
hdiutil('convert ' + dmg + '.tmp -format UDZO -o ' + dmg)
eject(dmg)
os.remove(dmg + '.tmp')
