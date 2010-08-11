#!/usr/bin/python2.6

import os
import re

for root, dirs, files in os.walk('.'):
    if not root == '.' and '.git' in dirs:
        print 'Skipping submodule', root
        continue
    for filename in [ x for x in files if re.search('\.java$',x) ]:
        package = None
        with open(os.path.join(root,filename)) as f:
            for line in f:
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
