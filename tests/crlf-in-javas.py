#!/bin/sh
''''exec "$(dirname "$0")"/../ImageJ --jython "$0" "$@" # (call again with fiji)'''


from java.io import BufferedReader, InputStreamReader

from java.lang import Runtime, System

import os, sys

def check_text(lines_array):
	for line in lines_array:
		if line.endswith("\r"):
			return False
	return True

def check_file(path):
	f = open(path, 'r')
	if not check_text(f.readlines()):
		f.close()
		print 'CR/LF detected in', path
		return
	f.close()

def check_directory(dir):
	for item in os.listdir(dir):
		path = dir + '/' + item
		if item.endswith('.java'):
                        check_file(path)
                elif os.path.isdir(path):
                        check_directory(path)

def execute(cmd):
	runtime = Runtime.getRuntime()
	p = runtime.exec(cmd)
	p.outputStream.close()
	result = ""
	reader = BufferedReader(InputStreamReader(p.inputStream))
	errorReader = BufferedReader(InputStreamReader(p.errorStream))
	while True:
		if p.errorStream.available() > 0:
			print errorReader.readLine()
		line=reader.readLine()
		if line == None:
			break
		result+=line + "\n"
	while True:
		line = errorReader.readLine()
		if line == None:
			break
		print line
	p.waitFor()
	if p.exitValue() != 0:
		print result
		raise RuntimeError, 'execution failure'
	return result

def check_in_HEAD(path):
	if not check_text(execute('git show HEAD:' + path).split("\n")):
		print 'CR/LF detected in', path

def check_HEAD():
	for line in execute('git ls-tree -r HEAD').split("\n"):
		if line == '':
			continue
		path = line.split("\t")[1]
		if path.endswith('.java'):
			check_in_HEAD(path)

ij_dir = System.getProperty('ij.dir') + '/src-plugins'

if sys.argv[0] == 'worktree':
	check_directory(ij_dir)
else:
	check_HEAD()
