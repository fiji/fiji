#!/bin/sh

URL=http://pacific.mpi-cbg.de
MIRROR=http://valelab.ucsf.edu/~schindelin
HOST=stuurman
DIR=public_html

DOWNLOADS=$(curl $URL/wiki/index.php/Downloads |
	sed -n 's|^.*a href="'$URL'/downloads/\([^/]*\).*$|\1|p' |
	sort |
	uniq |
	sed 's|^|/var/www/downloads/|')

rsync --progress -vau $DOWNLOADS $HOST:$DIR/downloads/ &&

curl $URL/wiki/index.php/Downloads |
sed -e "s|$URL/downloads|$MIRROR/downloads|g" \
	-e 's~\("\|(\)/\(wiki\|fiji-\)~\1'$URL'/\2~g' |
ssh $HOST "cat > $DIR/index.html"
