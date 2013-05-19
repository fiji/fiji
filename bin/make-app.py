#!/bin/sh
''''exec "$(dirname "$0")"/ImageJ.sh --jython "$0" "$@" # (call again with fiji)'''

import os
import shutil
import sys
from compat import chmod, execute
from java.io import File

if len(sys.argv) < 3:
	print 'Usage: ' + sys.argv[0] + ' <platform> <host-platform>'
	exit(1)

platform = sys.argv[1].replace('app-', '')
host_platform = sys.argv[2]

all_platforms = ['linux32', 'linux64', 'win32', 'win64', 'macosx']

if platform == 'nojre':
	copy_jre = False
	platform = 'all'
else:
	copy_jre = True


def removedirs(dir):
	if not isinstance(dir, File):
		dir = File(dir)
	list = dir.listFiles()
	if list is None:
		return
	for file in list:
		if file.isDirectory():
			removedirs(file)
		elif file.isFile():
			file.delete();
	dir.delete()

def make_app():
	print 'Making app'
	if os.path.isdir('Fiji.app'):
		removedirs('Fiji.app')
	os.makedirs('Fiji.app/images')
	shutil.copy('images/icon.png', 'Fiji.app/images/')
	for d in ['plugins', 'macros', 'jars', 'retro', 'luts', \
			'scripts']:
		shutil.copytree(d, 'Fiji.app/' + d)
	if os.path.isdir('samples'):
		shutil.copytree('samples', 'Fiji.app/samples')
	if os.path.isdir('Fiji.app/jars/jython2.2.1/cachedir'):
		removedirs('Fiji.app/jars/jython2.2.1/cachedir')
	if os.path.isdir('Fiji.app/jars/cachedir'):
		removedirs('Fiji.app/jars/cachedir')

def get_java_platform(platform):
	if platform == 'linux64':
		platform = 'linux-amd64'
	elif platform == 'macosx':
		platform = 'macosx-java3d'
	return platform

def find_java_tree(platform):
	java = 'java/' + platform
	revision = execute('git rev-parse HEAD:' + java)
	if platform == 'macosx-java3d':
		return [revision.replace('\n', ''), platform]
	tree = execute('git --git-dir=' + java + '/.git ls-tree ' + revision)
	return [tree[12:52] + ':jre',
		platform + '/' + tree[53:].replace('\n', '') + '/jre']

def copy_java(platform):
	if platform == 'linux32':
		platform = 'linux'
	java_platform = get_java_platform(platform)
	java_tree = find_java_tree(java_platform)
	os.system('git --git-dir=java/' + java_platform + '/.git ' \
		+ 'archive --prefix=Fiji.app/java/' + java_tree[1] + '/ ' \
			+ java_tree[0] + ' | ' \
			+ 'tar xf -')

def copy_platform_specific_files(platform):
	if copy_jre:
		print 'Copying Java files for', platform
		copy_java(platform)

	print 'Copying platform-specific files for', platform, \
		'(host platform=' + host_platform + ')'
	if platform == 'macosx':
		macos='Fiji.app/Contents/MacOS/'
		os.makedirs(macos)
		shutil.copy('Contents/MacOS/ImageJ-macosx', macos + 'ImageJ-macosx')
		shutil.copy('Contents/MacOS/ImageJ-tiger', macos)
		chmod(macos + 'ImageJ-macosx', 0755)
		chmod(macos + 'ImageJ-tiger', 0755)
		shutil.copy('Contents/Info.plist', 'Fiji.app/Contents/')
		images='Fiji.app/Contents/Resources/'
		os.makedirs(images)
		shutil.copy('Contents/Resources/Fiji.icns', images)
	else:
		if platform.startswith('win'):
			exe = ".exe"
		else:
			exe = ''

		binary = 'ImageJ-' + platform + exe
		shutil.copy(binary, 'Fiji.app/' + binary)
		chmod('Fiji.app/' + binary, 0755)

make_app()
execute('bin/download-launchers.sh snapshot')
if platform == 'all':
	for p in all_platforms:
		copy_platform_specific_files(p)
else:
	copy_platform_specific_files(platform)
