#!/bin/sh
''''exec "$(dirname "$0")"/../ImageJ --jython "$0" "$@" # (call again with fiji)'''

import os
import sys

from compat import execute

# TODO: allow other cwds than fiji/
# TODO: use JGit

if len(sys.argv) < 2:
	print 'Usage:', sys.argv[0], 'src-plugins/<path>...'
	sys.exit(1)

list = list()
third_parties = dict()
for file in sys.argv[1:]:
	if file.startswith('staged-plugins/'):
		if file.endswith('.jar'):
			file = file[15:]
			list.append(file)
			third_parties[file] = file
		else:
			print 'Will not add non-jar staged plugin'
		continue
	if not file.startswith('src-plugins/'):
		print 'Will not add plugin outside src-plugins:', file
		continue
	if file.find('_') < 0:
		print 'This is not a plugin:', file
		continue
	if not os.path.isdir(file) and not file.endswith('.java'):
		print 'Will not add non-Java file:', file
		continue
	list.append(file[12:])

# read .gitignore

ignored = dict()
f = open('.gitignore', 'r')
line_number = 0
for line in f.readlines():
	ignored[line] = line_number
	line_number += 1
f.close()

# read Fakefile

f = open('Fakefile', 'r')
fakefile = f.readlines()
f.close()
faked_plugins = dict()
last_plugin_line = -1
last_jar_plugin_line = -1
last_3rd_party_plugin_line = -1
for i in range(0, len(fakefile)):
	if fakefile[i].startswith('PLUGIN_TARGETS='):
		while i < len(fakefile) and fakefile[i] != "\n":
			if fakefile[i].endswith(".class \\\n"):
				last_plugin_line = i
				faked_plugins[fakefile[i]] = i
			elif fakefile[i].endswith(".jar \\\n"):
				last_jar_plugin_line = i
				faked_plugins[fakefile[i]] = i
			i += 1
	elif fakefile[i].startswith('THIRD_PARTY_PLUGINS='):
		while i < len(fakefile) and fakefile[i] != "\n":
			if fakefile[i].endswith(".jar \\\n"):
				last_3rd_party_plugin_line = i
				faked_plugins[fakefile[i]] = i
			i += 1

# remove all .class files in the given directory

def remove_class_files(dir):
	for item in os.listdir(dir):
		path = dir + '/' + item
		if item.endswith('.class'):
			os.remove(path)
		elif os.path.isdir(path):
			remove_class_files(path)

# add the plugin to .gitignore, Fakefile, and the file itself

def add_plugin(plugin):
	if plugin.endswith('.java'):
		target = 'plugins/' + plugin[0:len(plugin) - 5] + '.class'
	elif plugin in third_parties:
		target = 'plugins/' + plugin
	else:
		if plugin.endswith('/'):
			plugin = plugin[0:len(plugin) - 1]
		remove_class_files('src-plugins/' + plugin)
		target = 'plugins/' + plugin + '.jar'

	ignore_line = '/' + target + "\n"
	if not ignore_line in ignored:
		f = open('.gitignore', 'a')
		f.write(ignore_line)
		f.close()
		ignored[target] = -1
		execute('git add .gitignore')

	plugin_line = "\t" + target + " \\\n"
	global last_plugin_line, last_jar_plugin_line, faked_plugins
	global last_3rd_party_plugin_line
	if not plugin_line in faked_plugins:
		if plugin.endswith('.java'):
			if last_jar_plugin_line > last_plugin_line:
				last_jar_plugin_line += 1
			if last_3rd_party_plugin_line > last_plugin_line:
				last_3rd_party_plugin_line += 1
			last_plugin_line += 1
			fakefile.insert(last_plugin_line, plugin_line)
		elif plugin in third_parties:
			if last_plugin_line > last_3rd_party_plugin_line:
				last_plugin_line += 1
			if last_jar_plugin_line > last_3rd_party_plugin_line:
				last_jar_plugin_line += 1
			last_3rd_party_plugin_line += 1
			fakefile.insert(last_3rd_party_plugin_line, plugin_line)
		else:
			if last_plugin_line > last_jar_plugin_line:
				last_plugin_line += 1
			if last_3rd_party_plugin_line > last_jar_plugin_line:
				last_3rd_party_plugin_line += 1
			last_jar_plugin_line += 1
			fakefile.insert(last_jar_plugin_line, plugin_line)

		f = open ('Fakefile', 'w')
		f.write(''.join(fakefile))
		f.close()
		execute('git add Fakefile')

	if plugin in third_parties:
		file = 'staged-plugins/' + plugin
		third_party = 'third-party '
	else:
		file = 'src-plugins/' + plugin
		third_party = ''
	if execute('git ls-files ' + file) == '':
		action = 'Add'
	else:
		action = 'Modify'
	execute('git add ' + file)
	f = open('.msg', 'w')
	if plugin.endswith('.java'):
		plugin = plugin[0:len(plugin) - 5]
	elif plugin.endswith('.jar'):
		plugin = plugin[0:len(plugin) - 4]
	configfile = 'staged-plugins/' + plugin + '.config'
	if os.path.exists(configfile):
		execute('git add ' + configfile)
	name = plugin.replace('/', '>').replace('_', ' ')
	f.write(action + ' the ' + third_party + 'plugin "' + name + '"')
	f.close() 
	execute('git commit -s -F .msg')
	os.remove('.msg')

for plugin in list:
	print 'Adding', plugin
	add_plugin(plugin)
