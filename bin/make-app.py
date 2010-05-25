#!/bin/sh
''''exec "$(dirname "$0")"/../fiji --jython "$0" "$@" # (call again with fiji)'''

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
	for d in ['plugins', 'macros', 'jars', 'misc', 'retro', 'luts', \
			'scripts']:
		shutil.copytree(d, 'Fiji.app/' + d)
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
		if (host_platform == "osx10.5"):
			shutil.copy('fiji', macos + 'fiji-macosx')
			shutil.copy('fiji-tiger', macos)
		else:
			shutil.copy('precompiled/fiji-macosx',
					macos + 'fiji-macosx')
			shutil.copy('precompiled/fiji-tiger', macos)
		chmod(macos + 'fiji-macosx', 0755)
		chmod(macos + 'fiji-tiger', 0755)
		shutil.copy('Info.plist', 'Fiji.app/Contents/')
		images='Fiji.app/Contents/Resources/'
		os.makedirs(images)
		shutil.copy('images/Fiji.icns', images)
	else:
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
		copy_platform_specific_files(p)
else:
	copy_platform_specific_files(platform)
