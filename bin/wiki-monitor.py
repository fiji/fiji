#!/bin/sh
''''exec "$(dirname "$0")"/../fiji --jython --headless --mem=64m "$0" "$@" # (call again with fiji)'''

# This script allows you to monitor a Wiki conveniently, by looking at the
# Special:RecentChanges page, and comparing it with the version it found
# last time.
#
# Call the script with the URL of the Wiki's index.php as only argument. If
# the Wiki requires you to log in to see the recent changes, you can add the
# credentials to your $HOME/.netrc. The cached recent changes will be stored
# in the file ".recent-changes.<host>" in your current working directory.

from sys import argv, exit

if len(argv) != 2:
	print 'Usage:', argv[0], '<URL>'
	exit(1)

url = argv[1]

# get username and password
user = None
password = None
from os import getenv, path
home = getenv('HOME')
if home != None and path.exists(home + '/.netrc'):
	host = url
	if host.startswith('http://'):
		host = host[7:]
	elif host.startswith('https://'):
		host = host[8:]
	slash = host.find('/')
	if slash > 0:
		host = host[:slash]

	found = False
	f = open(home + '/.netrc')
	for line in f.readlines():
		line = line.strip()
		if line == 'machine ' + host:
			found = True
		elif found == False:
			continue
		elif line.startswith('login '):
			user = line[6:]
		elif line.startswith('password '):
			password = line[9:]
		elif line.startswith('machine '):
			break
	f.close()

from fiji import MediaWikiClient

client = MediaWikiClient(url)
if user != None and password != None and not client.isLoggedIn():
	client.logIn(user, password)
response = client.sendRequest(['title', 'Special:RecentChanges'], None)
if client.isLoggedIn():
	client.logOut()

'''
f = open('a1', 'r')
response = ''.join(f.readlines())
f.close()
'''

result = ''
for line in response.split('\n'):
	i = line.find('<h4>')
	if i >= 0:
		line = line[i + 4:]
		if line.endswith('</h4>'):
			line = line[:-5]
		if len(result) > 0:
			result += '\n'
		result += line + '\n'
	elif line.find('<li>') >= 0 or line.find('<li class=') >= 0:
		title = '<unknown>'
		time = '<sometime>'
		i = line.find('mw-userlink')
		if i > 0:
			start = line.find('>', i) + 1
			end = line.find('<', start)
			author = line[start:end]
			end = line.rfind('</a>', 0, i)
			start = line.rfind('>', 0, end) + 1
			title = line[start:end]
			start = line.find(';', end) + 1
			if start > 0:
				if line[start:].startswith('&#32;'):
					start += 5
				if line[start] == ' ':
					start += 1
				time = line[start:start + 5]
		else:
			author = '<unknown>'
		i = line.find('uploaded "<a href=')
		if i > 0:
			start = line.find('>', i) + 1
			end = line.find('<', start)
			title = ' -> ' + line[start:end]
		i = line.find('uploaded a new version of "')
		if i > 0:
			start = line.find('>', i) + 1
			end = line.find('<', start)
			title = ' ->> ' + line[start:end]
		result += '\t' + time + ' ' + title + ' (' + author + ')\n'

firstLine = 'From ' + url + '/Special:RecentChanges\n'
from java.lang import System
backup = '.recent-changes.' + host
if path.exists(backup):
	f = open(backup, 'r')
	firstline = f.readline().strip()
	secondline = f.readline().strip()
	f.close()
	lines = result.split('\n')
	if len(lines) > 0 and lines[0] == firstline:
		if len(lines) > 1 and lines[1].strip() == secondline:
			firstline = None
		else:
			firstline = secondline
	if firstline != None:
		for line in lines:
			if line.strip() == firstline:
				break
			else:
				if firstLine != None:
					System.out.println(firstLine)
					firstLine = None
				System.out.println(line)
else:
	System.out.println(firstLine)
	System.out.println(result)

f = open(backup, 'w')
f.write(result)
f.close()
