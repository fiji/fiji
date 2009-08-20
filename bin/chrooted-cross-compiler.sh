#!/bin/sh

NAME=dapper
CHROOT=chroot-$NAME-i386
CHROOT_HOME=/home/$USER

test -d $CHROOT || {
	mkdir $CHROOT &&
	sudo apt-get install -q -y --force-yes dchroot debootstrap &&
	sudo debootstrap --arch i386 $NAME \
		./$CHROOT/ http://archive.ubuntu.com/ubuntu &&
	for i in passwd shadow group sudoers hosts
	do
		sudo cp /etc/$i ./$CHROOT/etc/
	done &&

	(grep "$(pwd)/$CHROOT" /etc/dchroot.conf 2> /dev/null ||
	 sudo sh -c "echo \"$NAME $(pwd)/$CHROOT\" >> /etc/dchroot.conf") &&

	sudo mkdir $CHROOT/$CHROOT_HOME &&
	sudo chown $USER.$USER $CHROOT/$CHROOT_HOME || {
		echo "Could not make chroot"
		exit 1
	}
}

test -f $CHROOT/usr/bin/wget ||
dchroot "sudo apt-get install -q -y --force-yes gcc libcurl3-openssl-dev make \
	libexpat-dev perl-modules tk8.4 g++ pax patch \
	autoconf automake libtool bison flex unzip make wget" || {
	echo "Could not install packages"
	exit 1
}

(cd $CHROOT/$CHROOT_HOME &&
 if test -d IMCROSS
 then
	cd IMCROSS
	git rev-parse --verify refs/heads/fiji 2>/dev/null >/dev/null ||
	git checkout -b fiji origin/fiji
	git pull
 else
	git clone git://pacific.mpi-cbg.de/IMCROSS/.git
	cd IMCROSS
	git checkout -b fiji origin/fiji
 fi)

test -f $CHROOT/opt/mac/bin/i686-apple-darwin8-g++ \
	-a -f $CHROOT/usr/local/bin/i386-mingw32-g++ ||
dchroot "cd IMCROSS && sudo make fiji"

SOURCE=fiji.cxx
STRIP=
RESOURCE=
PLATFORM="$1"; shift
case "$PLATFORM" in
win32)
	CXX=/usr/local/bin/i386-mingw32-g++
	STRIP=/usr/local/bin/i386-mingw32-strip
	TARGET=fiji-win32.exe

	echo "101 ICON fiji.ico" > $CHROOT/$CHROOT_HOME/tmp.rc
	cp images/fiji.ico $CHROOT/$CHROOT_HOME
	dchroot "/usr/local/bin/i386-mingw32-windres -i tmp.rc -o tmp.o"
	RESOURCE=tmp.o
;;
tiger)
	CXX=/opt/mac/bin/i686-apple-darwin8-g++
	TARGET=fiji-tiger
;;
linux)
	CXX=/usr/bin/g++
	TARGET=fiji-linux
;;
*)
	echo "Unknown platform: $PLATFORM!" >&2
	exit 1
;;
esac

test "a$1" = "a-o" && {
	TARGET="$2"
	shift; shift
}

QUOTED_ARGS="$(echo "$*" | sed 's/"/\\"/g')"

cp -R $SOURCE includes $CHROOT/$CHROOT_HOME/

dchroot "$CXX -o \"$TARGET\" $QUOTED_ARGS $SOURCE $RESOURCE"

test -z "$STRIP" || dchroot "$STRIP \"$TARGET\""

cp $CHROOT/$CHROOT_HOME/"$TARGET" ./precompiled/
