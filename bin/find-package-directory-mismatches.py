#!/bin/sh
''''exec "$(dirname "$0")"/../fiji --jython "$0" "$@" # (call again with fiji)'''

import os
import re

submodules = set()

def is_in_submodule(name):
    for submodule in submodules:
        if name.startswith(submodule):
            return True
    return False

for root, dirs, files in os.walk('.'):
    if is_in_submodule(root):
        continue
    if not root == '.' and '.git' in dirs:
        print 'Skipping submodule', root
        submodules.add(root + '/')
        continue
    for filename in [ x for x in files if re.search('\.java$',x) ]:
        package = None
        for line in open(os.path.join(root,filename)):
            m = re.search('^\s*package\s*([\S^;]+)\s*;',line)
            if m:
                package = m.group(1)
                break
        if package:
            package_parts = package.split('.')
            directory_left = root[:]
            while len(package_parts) > 0:
                directory_left, basename = os.path.split(directory_left)
                last_package_part = package_parts.pop()
                if basename != last_package_part:
                    print "Directory was wrong for: "+os.path.join(root,filename)
                    print "    package declaration was: "+package
                    print "    "+basename+" did not match "+last_package_part
