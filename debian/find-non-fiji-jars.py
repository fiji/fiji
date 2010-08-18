#!/usr/bin/python2.6

import os
import sys
import re

script_directory = sys.path[0]
if not script_directory:
    raise Exception, "Couldn't find the directory in which the script lives"

build_command_script = os.path.join(script_directory,'build-command')

jars = set()

with open(build_command_script) as f:
    for line in f:
        matches = re.findall('CLASSPATH\(.*?\)=([\w\.\:/\-]+)',line)
        for m in matches:
            for j in m.split(':'):
                if re.search('^/',j):
                    jars.add(j)

print ':'.join(jars)
