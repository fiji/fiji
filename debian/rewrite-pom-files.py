#!/usr/bin/python

import sys, os, json, tempfile, shutil
from lxml import etree
from optparse import OptionParser

nss = { 'mvn': 'http://maven.apache.org/POM/4.0.0' }
top_level = os.path.realpath(os.path.join(sys.path[0], '..'))

parser = OptionParser()
parser.add_option('-r', '--remove',
                  dest="remove",
                  default=False,
                  action='store_true',
                  help="Remove unknown artifacts")
options,args = parser.parse_args()

required_packages = set([])

def rewrite_pom(pom_filename, mapping):
    identifiers = ['groupId', 'artifactId', 'version']
    identifiers_with_namespace = [ ('{%(mvn)s}' % nss) + x for x in identifiers ]

    tree = etree.parse(pom_filename)

    for e in tree.xpath('//mvn:dependency', namespaces=nss):
        values = []
        for x in identifiers:
            matches = e.xpath('./mvn:'+x, namespaces=nss)
            if matches:
                if len(matches) > 1:
                    raise Exception, "Got an unexpected number of matches (%d) for %s" % (len(matches), x)
                values.append(matches[0].text)
            else:
                values.append('')
        combined =  ':'.join(values)
        groupId, artifactId, version = values
        if groupId == '${project.groupId}':
            continue
        if (combined in mapping) and ('error' not in mapping[combined]):
            for child in e:
                if child.tag not in identifiers_with_namespace:
                    e.remove(child)
            scope = etree.SubElement(e, 'scope')
            scope.text = 'system'
            system_path = etree.SubElement(e, 'systemPath')
            system_path.text = mapping[combined]['jar']
            required_packages.add(mapping[combined]['package'])
        else:
            print pom_filename, ": failed to find", combined
            if options.remove:
                e.getparent().remove(e)
    output = tempfile.NamedTemporaryFile(delete=False)
    output.write(etree.tostring(tree,
                                pretty_print=True,
                                xml_declaration=True,
                                encoding="UTF-8"))
    print "Wrote output to:", output.name
    shutil.move(output.name, pom_filename)

with open(os.path.join(top_level, 'debian', 'mapping.json')) as fp:
    mapping = json.load(fp)

for dirpath, dirnames, filenames in os.walk(top_level):
    if 'pom.xml' in filenames:
        pom_filename = os.path.join(dirpath, 'pom.xml')
        print 'Will rewrite:', pom_filename
        rewrite_pom(pom_filename, mapping)
