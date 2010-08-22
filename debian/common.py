import os
import re
from subprocess import Popen, PIPE
from htmlentitydefs import name2codepoint

chroot_path = "/var/chroot/squeeze-i386"
home_in_chroot = os.path.join(chroot_path,"home/mark")
build_path_in_chroot = os.path.join(home_in_chroot,"fiji-build")
owner_and_group = "mark.mark"

def get_version_from_changelog(changelog_path):

    p = Popen(["dpkg-parsechangelog","-l"+changelog_path],stdout=PIPE)
    changelog = p.communicate()[0]
    if p.returncode != 0:
        raise Exception, "Failed to parse debian/changelog"

    version_match = re.search("(?ims)Version: (\S+)",changelog)
    if not version_match:
        raise Exception, "Failed to find the Version field"

    version_from_changelog = version_match.group(1)

    if not re.match('^\d{14}$',version_from_changelog):
        print >> sys.stderr, "Error: The version number is not of the form YYYYMMDDHHMMSS"
        sys.exit(6)

    return version_from_changelog

# These next two useful functions are from:
#   http://snippets.dzone.com/posts/show/4569

def substitute_entity(match):
    ent = match.group(2)
    if match.group(1) == "#":
	return unichr(int(ent))
    else:
        cp = name2codepoint.get(ent)
	if cp:
            return unichr(cp)
        else:
            return match.group()

def decode_htmlentities(string):
    entity_re = re.compile("&(#?)(\d{1,5}|\w{1,8});")
    return entity_re.subn(substitute_entity, string)[0]

def trim_line(s,max_length):
    rs = s.rstrip()
    if len(rs) <= max_length:
        return rs
    else:
        substitution = " [...]"
        return rs[0:(max_length-len(substitution))].rstrip() + substitution

def classpath_definitions_from(filename):
    result = {}
    with open(filename) as f:
        for line in f:
            matches = re.findall('CLASSPATH\((.*?)\)=([\w\.\:/\-\$]+)',line)
            for m in matches:
                target_filename = m[0]
                result.setdefault(target_filename,[])
                for j in m[1].split(':'):
                    result[target_filename].append(j)
    return result
