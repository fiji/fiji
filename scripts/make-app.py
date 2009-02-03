#!/usr/bin/python

import os
import shutil
import sys
from compat import removedirs, chmod, execute

if len(sys.argv) < 3:
	print 'Usage: ' + sys.argv[0] + ' <platform> <host-platform>'
	exit(1)

platform = sys.argv[1].replace('app-', '')
host_platform = sys.argv[2]

all_platforms = ['linux', 'linux64', 'win32', 'win64', 'macosx']

if platform == 'nojre':
	copy_jre = False
	platform = 'all'
else:
	copy_jre = True


def make_app():
	print 'Making app'
	if os.path.isdir('Fiji.app'):
		removedirs('Fiji.app')
	os.makedirs('Fiji.app/images')
	shutil.copy('images/icon.png', 'Fiji.app/images/')
	shutil.copy('ij.jar', 'Fiji.app/')
	for d in ['plugins', 'macros', 'jars', 'misc', 'retro']:
		shutil.copytree(d, 'Fiji.app/' + d)
	if os.path.isdir('Fiji.app/jars/jython2.2.1/cachedir'):
		removedirs('Fiji.app/jars/jython2.2.1/cachedir')

def add_macosx_app():
	if copy_jre:
		os.system('git archive --prefix=Fiji.app/java/macosx-java3d/ ' \
			+ 'origin/java/macosx-java3d: | ' \
			+ 'tar xvf -')

	macos='Fiji.app/Contents/MacOS/'
	os.makedirs(macos)
	if (host_platform == platform):
		shutil.copy('fiji', macos + 'fiji-macosx')
		shutil.copy('fiji-tiger', macos)
	else:
		shutil.copy('precompiled/fiji-macosx', macos + 'fiji-macosx')
		shutil.copy('precompiled/fiji-tiger', macos)
	chmod(macos + 'fiji-macosx', 0755)
	chmod(macos + 'fiji-tiger', 0755)
	shutil.copy('Info.plist', 'Fiji.app/Contents/')
	images='Fiji.app/Contents/Resources/'
	os.makedirs(images)
	shutil.copy('images/Fiji.icns', images)

def find_java_tree(platform):
	if platform == 'linux64':
		platform = 'linux-amd64'
	java = 'origin/java/' + platform
	version = execute('git ls-tree --name-only ' + java).replace('\n', '')
	return java + ':' + version + '/jre'

def add_other_app(platform):
	if copy_jre:
		java_tree = find_java_tree(platform)
		java = java_tree.replace(':', '/').replace('origin/', '')

		os.system('git archive --prefix=Fiji.app/' + java + '/ ' \
			+ java_tree + ' | ' \
			+ 'tar xvf -')

	if platform.startswith('win'):
		exe = ".exe"
	else:
		exe = ''

	binary = 'fiji-' + platform + exe
	if (host_platform == platform):
		shutil.copy('fiji' + exe, 'Fiji.app/' + binary)
	else:
		shutil.copy('precompiled/' + binary, 'Fiji.app/' + binary)
	chmod('Fiji.app/' + binary, 0755)

make_app()
if platform == 'all':
	for p in all_platforms:
		if p == 'macosx':
			add_macosx_app()
		else:
			add_other_app(p)
elif platform == "macosx":
	add_macosx_app()
else:
	add_other_app(platform)
