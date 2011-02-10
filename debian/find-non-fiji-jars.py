#!/usr/bin/python

import os
import sys
import re

from common import *

script_directory = sys.path[0]
if not script_directory:
    raise Exception, "Couldn't find the directory in which the script lives"

build_command_script = os.path.join(script_directory,'build-command')

jars = set()

classpath_definitions = classpath_definitions_from(build_command_script)

for file_to_build, dependents in classpath_definitions.items():
    for j in dependents:
        if re.search('^/',j):
            jars.add(j)

print ':'.join(jars)
