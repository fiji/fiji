#!/bin/sh

die () {
	echo "$*" >&2
	exit 1
}

test Linux = "$(uname -s)" ||
die "This script only runs on Linux (because it requires GNU sed)"

test $# = 1 ||
die "Usage: $0 <name>"
name="$1"

cd "$(dirname "$0")/.." ||
die "Could not cd to Fiji's top-level directory"

test -d src-plugins/"$name" ||
die "src-plugins/$name does not exist"

name2="${name%_}"
REPOURL=https://github.com/fiji/"$name2"
curl -f -s "$REPOURL" ||
curl -f --netrc -XPOST -d "{\"name\":\"$name2\"}" https://api.github.com/orgs/fiji/repos ||
die "Could not create $REPOURL"

git clone -b master . external/"$name" ||
die "Could not clone to external/$name"

(cd external/"$name" &&
  git filter-branch -f --prune-empty --subdirectory-filter src-plugins/"$name" &&

  git commit --allow-empty -s -m "Mark first commit of $name in its own repository" -m "Based on $(git log --format='%h(%s)' -1 origin/master -- src-plugins/"$name")." &&

  sed -i "s|[ \t]*<repositories>|\t<scm>\n\t\t<connection>scm:git:git://github.com/fiji/$name2</connection>\n\t\t<developerConnection>scm:git:git@github.com:fiji/$name2</developerConnection>\n\t\t<tag>HEAD</tag>\n\t\t<url>$REPOURL</url>\n\t</scm>\n\n&|" pom.xml &&
  git commit -s -m "Add SCM location" pom.xml &&

  sed -i "s|^\(\t\t<artifactId>pom-fiji\)-plugins\(</artifactId>\)|\1\2|" pom.xml &&
  git commit -s -m "Switch the parent to pom-fiji" pom.xml &&

  cat > .gitignore << EOF &&
# Eclipse #
/.classpath
/.project
/.settings/

# Maven #
/target/
EOF
  git add .gitignore &&
  git commit -s -m "Add a .gitignore file" .gitignore &&

  git remote set-url origin git@github.com:fiji/"$name2" &&
  git push origin HEAD) &&

version="$(sed -n 's|^\t<version>\(.*\)</version>|\1|p' < external/"$name"/pom.xml)" &&
version="${version:-2.0.0-SNAPSHOT}" &&
sed -i "s|^\t</properties>|\t\t<$name.version>$version</$name.version>\n&|" pom.xml &&
sed -i "/<artifactId>$name</{N;s/\(<version>\).*\(<\/version>\)/\1\${$name.version}\2/}" src-plugins/*/pom.xml &&
sed -i "s|^\t</dependencies>|\t\t<dependency>\n\t\t\t<groupId>sc.fiji</groupId>\n\t\t\t<artifactId>$name</artifactId>\n\t\t\t<version>\${$name.version}</version>\n\t\t\t<scope>runtime</scope>\n\t\t</dependency>\n&|" external/pom.xml &&
sed -i "/<module>$name<\/module>/d" src-plugins/pom.xml &&
git rm -rf src-plugins/"$name" &&
git commit -s -m "$name is an external project now" -m "As of $(cd external/"$name" && git log --format='%h(%s)' -1), $name lives in" -m "	$REPOURL" pom.xml src-plugins/*/pom.xml external/pom.xml src-plugins/pom.xml src-plugins/"$name" &&

echo "/$name/" >> external/.gitignore &&
git commit -s -m "Ignore if $name is cloned into external/" external/.gitignore &&

perl -i -e 'my $line = undef;
my $in_minimaven_section = 0;
while (<>) {
	if (/^.*\/'"$name"'-.*\.jar$/) {
		$line = $_;
		next;
	}
	if ($in_minimaven_section) {
		if ($_ eq '' || ($line cmp $_) < 0) {
			print $line;
			$in_minimaven_section = 0;
		}
	} elsif (/^# Dependencies copied by MiniMaven #$/) {
		$in_minimaven_section = 1;
	}
	print $_;
}' .gitignore &&
git commit -s -m "Adjust .gitignore" .gitignore &&
echo "Now, test and push" ||
die "Uh oh!"
