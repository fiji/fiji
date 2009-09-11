#!/usr/bin/python

from sys import argv
from fiji.updater.logic import UpdateFiji

if argv[0] != 'update':
	argv.insert(1, 'update')
UpdateFiji.main(argv[1:])
