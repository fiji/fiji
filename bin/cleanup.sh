#!/bin/sh

die () {
	echo "$*" >&2
	exit 1
}

is_dirty () {
	test ! -z "$(git diff --name-only --ignore-submodules;
		git diff --cached --name-only --ignore-submodules)"
}

is_dirty && die 'Dirty working directory.  Aborting.'

# mark non-executables as not executable
git ls-files --stage |
sed -ne '/\(\.\(sh\|exe\|py\)\|debian\/rules\)$/d' \
	-e 's/^100755 .\{43\}//p' |
while read file
do
	chmod 0644 "$file"
	git add "$file"
done

is_dirty && git commit -s -m "Mark some files non-executable"

# convert DOS/Mac line endings to Unix line endings
TOUNIX=ImageJA/tools/mac2unix.pl
test -x $TOUNIX &&
git ls-files |
grep -ve ^precompiled -e \\.jpg$ -e \\.gif$ -e \\.png$ -e \\.jar$ |
while read file
do
	test -x "$file" || ! grep '\r' "$file" > /dev/null && continue
	$TOUNIX "$file"
	git add "$file"
done

is_dirty && git commit -s -m "Fix line endings"
