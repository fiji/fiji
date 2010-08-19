import os
import re
from subprocess import Popen, PIPE

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
