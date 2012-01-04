#!/bin/sh
''''exec "$(dirname "$0")"/../ImageJ --jython "$0" "$@" # (call again with fiji)'''


from fiji import CheckClassVersions

from java.lang import System

ij_dir = System.getProperty('ij.dir') + '/'

dirs = ['plugins/', 'jars/', 'misc/', 'precompiled/']
dirs = [ij_dir + dir for dir in dirs]

CheckClassVersions().main(dirs)
