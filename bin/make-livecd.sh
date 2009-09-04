#!/bin/sh

# TODO: nvidia stuff

LIVECD=livecd
SVG=images/fiji-logo-1.0.svg
XPM=images/fiji.xpm.gz
LSS16=images/fiji16.lss
PNG16=images/fiji16.png
PACKAGES='kdebase kdebase-bin kicker klipper kdesktop kwin konqueror \
	kdm ksmserver knetworkmanager konsole xinit xserver-xorg'
IMAGE_FILE=fiji-live.iso

EXTRA_LH_CONFIG=
test a"$1" = a--usb && {
	shift
	EXTRA_LH_CONFIG="$EXTRA_LH_CONFIG --binary-images usb-hdd"
	IMAGE_FILE=fiji-usb.img
}


# some functions

# die <message>

die () {
	echo "$@" >&2
	exit 1
}

# upToDate <target> <source>

upToDate () {
	test -f "$1" && test ! "$2" -nt "$1"
}



# go to the Fiji root

cd "$(dirname "$0")"/..
FIJIROOT="$(pwd)"/

# Avoid relying on a buggy live-helper from Ubuntu:
export LH_BASE="$(pwd)"/live-helper

test -d "$LH_BASE" ||
die "live-helper package is not available"

for d in live-helper java/linux
do
	if test ! -d $d/.git
	then
		git submodule init $d &&
		git submodule update $d
	fi || break
done ||
die "Error checking out submodules"

# TODO: this depends on i386
test -x Fiji.app/fiji-linux ||
sh Fake.sh app-linux ||
die "Could not make Fiji for Linux/i386"

# make the logos

WIDTH=640; HEIGHT=400
upToDate $LSS16 $SVG ||
./fiji -eval 'run("SVG...", "choose='$SVG' width='$HEIGHT' height='$HEIGHT'");
	run("Canvas Size...", "width='$WIDTH' height='$HEIGHT' position=Center zero");
	run("8-bit Color", "number=16");
	run("LSS16 ...", "save='$LSS16'");' -batch ||
die "Could not make $LSS16"


WIDTH=640; HEIGHT=480
upToDate $PNG16 $SVG ||
./fiji -eval 'run("SVG...", "choose='$SVG' width='$HEIGHT' height='$HEIGHT'");
	run("Canvas Size...", "width='$WIDTH' height='$HEIGHT' position=Center zero");
	run("8-bit Color", "number=16");
	saveAs("PNG ...", "'$PNG16'");' -batch ||
die "Could not make $PNG16"

WIDTH=640; HEIGHT=320
upToDate $XPM $SVG || {
XPM2=$(dirname $XPM)/$(basename $XPM .gz)
./fiji -eval 'run("SVG...", "choose='$SVG' width='$HEIGHT' height='$HEIGHT'");
	run("Canvas Size...", "width='$WIDTH' height='$HEIGHT' position=Center zero");
	run("8-bit Color", "number=92");
	run("XPM ...", "save='$XPM2'");' -batch &&
gzip -9 $XPM2
} ||
die "Could not make $XPM"

# cp  /usr/share/live-helper/examples/hooks/nvidia-legacy.sh \
#	config/chroot_local-hooks/ &&

mkdir -p $LIVECD &&
(cd $LIVECD &&
 for i in dev/pts proc sys
 do
	sudo umount chroot/$i || true
 done &&
 sudo rm -rf binary* cache/stages_bootstrap/ chroot/ .stage/ .lock \
	config/chroot_local* &&
 "$LH_BASE"/helpers/lh_config --mode ubuntu \
	-p minimal \
	-a i386 \
	-d hardy \
	--initramfs casper \
	-k generic \
	--linux-packages="linux-image" \
	--apt-secure disabled \
	--bootstrap cdebootstrap \
	--grub-splash "$FIJIROOT"$XPM \
	--bootappend-live "quiet splash vga=785" \
	--iso-application "Fiji Live" \
	--iso-publisher "Fiji project; http://pacific.mpi-cbg.de" \
	--iso-volume "Fiji Live $(date +%Y%m%d-%H:%M)" \
	--syslinux-splash "$FIJIROOT"$LSS16 \
	--syslinux-timeout 5 \
	--username fiji \
	--packages "$PACKAGES" \
	$EXTRA_LH_CONFIG &&
 perl -pi.bak -e 's/LIVE_ENTRY=.*/LIVE_ENTRY="Start Fiji Live"/' \
	config/binary &&
 INCLUDES=config/chroot_local-includes &&
 USPLASH=/usr/local/lib/usplash &&
 mkdir -p $INCLUDES$USPLASH &&
 cp "$FIJIROOT"$PNG16 $INCLUDES$USPLASH/usplash-artwork.png &&
 cat > config/chroot_local-hooks/splash << EOF &&
#!/bin/sh

if test ! -s /etc/hosts
then
	host archive.ubuntu.com |
	sed "s/\(.*\) has address \(.*\)/\2 \1/" > /etc/hosts
fi

apt-get update &&
apt-get install --yes --force-yes $PACKAGES ||
exit

test -f $USPLASH/usplash-fiji.so &&
exit

apt-get install -q -y --force-yes libusplash-dev libc6-dev gcc usplash &&
(cd $USPLASH &&
 cat > usplash-fiji.c << 2EOF &&
#include <usplash-theme.h>
#include <usplash_backend.h>
2EOF
 pngtousplash usplash-artwork.png >> usplash-fiji.c &&
 cat >> usplash-fiji.c << 2EOF &&
struct usplash_theme usplash_theme = {
	.version = THEME_VERSION,
	.next = NULL,
	.ratio = USPLASH_4_3,

	.pixmap = &pixmap_usplash_artwork,

	.background             = 0x0,
	.progressbar_background = 0x7,
	.progressbar_foreground = 0x0,

	/* Progress bar position and size in pixels */
	.progressbar_x      = 5,
	.progressbar_y      = 479,
	.progressbar_width  = 630,
	.progressbar_height = 1,
};
2EOF
 gcc -I/usr/include/bogl -Os -g -fPIC -c usplash-fiji.c -o usplash-fiji.o &&
 gcc -shared -Wl,-soname,usplash-fiji.so usplash-fiji.o -o usplash-fiji.so &&
 rm usplash-fiji.[co]) &&
apt-get remove -q -y --force-yes libusplash-dev libc6-dev gcc &&
apt-get autoremove -q -y --force-yes &&
sudo mkdir -p /usr/lib/usplash &&
sudo ln -sf $USPLASH/usplash-fiji.so /usr/lib/usplash/usplash-artwork.so &&
echo xres=640 >> /etc/usplash.conf &&
echo yres=480 >> /etc/usplash.conf
EOF
 cat > config/chroot_local-hooks/names << EOF &&
#!/bin/sh

sudo perl -pi.bak -e 's/ubuntu/fiji/g' /etc/casper.conf &&
sudo rm /etc/casper.conf.bak
EOF
 mkdir -p $INCLUDES/usr/X11/Xsession.d &&
 cat > $INCLUDES/usr/X11/Xsession.d/91-fiji << EOF &&
#!/bin/sh

/usr/local/fiji/fiji-linux
EOF
 FIJITARGET=/usr/local/fiji &&
 mkdir -p $INCLUDES$FIJITARGET &&
 (cd "$FIJIROOT"Fiji.app && tar cvf - .) |
	(cd $INCLUDES$FIJITARGET && sudo tar xvf -) &&
 mkdir -p $INCLUDES/usr/bin &&
 sudo ln -s ../local/fiji/fiji-linux $INCLUDES/usr/bin/fiji &&
 mkdir -p $INCLUDES/usr/share/applications &&
 cat > $INCLUDES/usr/share/applications/Fiji.desktop << EOF &&
[Desktop Entry]
Type=Application
Encoding=UTF-8
Name=Fiji
GenericName=
Comment=
Icon=$FIJITARGET/images/icon.png
Exec=/usr/bin/fiji
Terminal=false
Categories=Graphics
EOF
 mkdir -p $INCLUDES/etc/profile.d &&
 cat > $INCLUDES/etc/profile.d/fiji.sh << \EOF &&
test -d $HOME/.kde/Autostart || {
	mkdir -p $HOME/.kde/Autostart &&
	ln -s /usr/local/fiji/fiji-linux $HOME/.kde/Autostart/fiji &&

	# work around a QEmu Cirrus emulation bug
	if dmesg | grep -q "QEMU DVD-ROM"
	then
		sudo sed -i.bak -e 's/Section "Screen"/&\
	DefaultDepth	16\
	SubSection	"Display"\
		Depth	16\
		Modes	"1024x768"\
	EndSubSection/' /etc/X11/xorg.conf
	fi
} || echo "Warning: could not make Autostart"
EOF
 chmod a+x $INCLUDES/etc/profile.d/fiji.sh &&
 sudo LH_BASE="$LH_BASE" PATH="$LH_BASE"/helpers:"$PATH" \
	"$LH_BASE"/helpers/lh_build &&
 mv -f binary.${IMAGE_FILE##*.} "$FIJIROOT"$IMAGE_FILE) ||
die "Building LiveCD failed"
