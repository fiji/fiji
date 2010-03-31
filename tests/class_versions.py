#!/bin/sh
''''exec "$(dirname "$0")"/../fiji --jython "$0" "$@" # (call again with fiji)'''


from fiji import CheckClassVersions

from java.lang import System

fiji_dir = System.getProperty('fiji.dir') + '/'

dirs = ['plugins/', 'jars/', 'misc/', 'precompiled/']
dirs = [fiji_dir + dir for dir in dirs]

CheckClassVersions().main(dirs)
