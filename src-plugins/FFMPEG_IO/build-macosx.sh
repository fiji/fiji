#!/bin/sh

cd "$(dirname "$0")"

TARGET=i686-apple-darwin8
TARGET64=x86_64-apple-darwin8
TARGETPPC=powerpc-apple-darwin8
SDK=MacOSX10.6u.sdk
SYSROOT="$(pwd)/mac-sysroot"
test -d "$SYSROOT" || mkdir -p "$SYSROOT"
PARALLEL=-j5

if test ! -f Payload
then
	cat << EOF
Need the file 'Payload', extracted on a Mac by
1) downloading xcode*.dmg
2) opening the .dmg
3) unpacking the SDK with

	xar -x -f /Volumes/Xcode/Packages/MacOSX10.6.pkg
EOF
	exit 1
fi

if test -f /usr/lib32/libstdc++.so.6
then
	error=false
	for lib in crypto stdc++
	do
		f=/usr/lib32/lib$lib.so
		if test ! -f $f
		then
			case $error in
			false)
				echo "Missing symlinks (32-bit libs):"
				error=true
				;;
			esac &&
			f2="$(ls $f.* | tail -n 1)"
			if test -f "$f2"
			then
				echo "sudo ln -s $f2 $f"
			else
				echo "error: $f is missing"
			fi
		fi
	done
	test $error = false || exit 1
fi

if test ! -f "$SYSROOT"/bin/$TARGET64-nm
then
	: need to compile libuuid for 32-bit &&
	if test ! -f "$SYSROOT/lib/libuuid.a"
	then
		if test ! -d util-linux-ng-2.16
		then
			curl -O http://archive.ubuntu.com/ubuntu/pool/main/u/util-linux/util-linux_2.16.orig.tar.gz &&
			curl -O http://archive.ubuntu.com/ubuntu/pool/main/u/util-linux/util-linux_2.16-1ubuntu5.diff.gz &&
			tar xzvf util-linux_2.16.orig.tar.gz &&
			(cd util-linux-ng-2.16 &&
			 gzip -d < ../util-linux_2.16-1ubuntu5.diff.gz | patch -p1)
		fi &&
		(cd util-linux-ng-2.16 &&
		 CFLAGS="-m32" LDFLAGS="-m32 -L/usr/lib32" \
		 ./configure --without-ncurses --disable-shared \
			--with-sysroot="$SYSROOT" --prefix="$SYSROOT" &&
		 cd shlibs/uuid &&
		 make $PARALLEL &&
		 make $PARALLEL install)
	fi &&
	if test ! -d odcctools
	then
		git clone git://pacific.mpi-cbg.de/iphone-dev odcctools &&
		(cd odcctools && git checkout -t origin/fiji)
	fi &&
	mkdir -p build-odcctools &&
	(cd build-odcctools &&
	 CFLAGS="-m32" LDFLAGS="-m32 -L/usr/lib32 -L$SYSROOT/lib" \
	 ../odcctools/configure \
		--target=$TARGET --enable-ld64 \
		--enable-targets=$TARGET,$TARGET64,$TARGETPPC \
		--with-sysroot="$SYSROOT" --prefix="$SYSROOT" &&
	 make $PARALLEL &&
	 make $PARALLEL install) &&
	mkdir -p build-odcctools64 &&
	(cd build-odcctools64 &&
	 CFLAGS="-m32" LDFLAGS="-m32 -L/usr/lib32 -L$SYSROOT/lib" \
	 ../odcctools/configure \
		--target=$TARGET64 --enable-ld64 \
		--with-sysroot="$SYSROOT" --prefix="$SYSROOT" &&
	 make $PARALLEL &&
	 make $PARALLEL install)
fi &&
if test ! -d "$SYSROOT/usr/$TARGET/System"
then
	(gzip -d < Payload | cpio -i) &&
	mkdir -p "$SYSROOT/usr/$TARGET" &&
	mv SDKs/MacOSX10.6.sdk/* "$SYSROOT/usr/$TARGET/"
fi &&
if test ! -f "$SYSROOT/bin/$TARGET-gcc"
then
	if test ! -f gcc-5664.tar.gz
	then
		curl -O http://www.opensource.apple.com/tarballs/gcc/gcc-5664.tar.gz
	fi &&
	if test ! -d gcc-5664
	then
		tar xzvf gcc-5664.tar.gz
	fi &&
	if ! grep '^BUILD_CFLAGS.*CFLAGS_FOR_BUILD' gcc-5664/gcc/Makefile.in
	then
		(cd gcc-5664 &&
		 cat > build-cflags.patch << \EOF &&
diff --git a/gcc/Makefile.in b/gcc/Makefile.in
index 8fe7ea1..c49fed2 100644
--- a/gcc/Makefile.in
+++ b/gcc/Makefile.in
@@ -676,7 +676,7 @@ DIR = ../gcc
 
 # Native compiler for the build machine and its switches.
 CC_FOR_BUILD = @CC_FOR_BUILD@
-BUILD_CFLAGS= @BUILD_CFLAGS@ -DGENERATOR_FILE
+BUILD_CFLAGS= @BUILD_CFLAGS@ $(CFLAGS_FOR_BUILD) -DGENERATOR_FILE
 
 # Native linker and preprocessor flags.  For x-fragment overrides.
 BUILD_LDFLAGS=$(LDFLAGS)
EOF
		 patch -p0 < build-cflags.patch)
	fi &&
	mkdir -p build-macgcc &&
	(cd build-macgcc &&
	 export PATH="$SYSROOT/bin:$PATH" &&
	 cat << EOF >> "$SYSROOT/bin/lipo" &&
#!/bin/sh

# Force multilib
echo x86_64
EOF
	 chmod a+x "$SYSROOT/bin/lipo" &&
	 CFLAGS="-m32" LDFLAGS="-m32 -L/usr/lib32 -L$SYSROOT/lib" \
	 ../gcc-5664/configure \
		--target=$TARGET --enable-ld64 \
		--enable-targets=$TARGET,$TARGET64,$TARGETPPC \
		--disable-checking --enable-languages=c,c++ \
		--with-as="$SYSROOT/bin/$TARGET-as" \
		--with-ld="$SYSROOT/bin/$TARGET-ld64" \
		--enable-static --enable-shared --disable-nls \
		--disable-multilib \
		--with-sysroot="$SYSROOT" --prefix="$SYSROOT" &&
	make $PARALLEL &&
	make $PARALLEL install)
fi &&
if test ! -f "$SYSROOT/bin/$TARGET64-gcc"
then
	mkdir -p build-macgcc64 &&
	(cd build-macgcc64 &&
	 export PATH="$SYSROOT/bin:$PATH" &&
	 CFLAGS_FOR_BUILD="-m32" CFLAGS="-m64" \
	 LDFLAGS="-L/usr/lib32 -L$SYSROOT/lib" \
	 ../gcc-5664/configure \
		--target=$TARGET64 --enable-ld64 \
		--disable-checking --enable-languages=c,c++ \
		--with-as="$SYSROOT/bin/$TARGET64-as" \
		--with-ld="$SYSROOT/bin/$TARGET64-ld64" \
		--enable-static --enable-shared --disable-nls \
		--disable-multilib \
		--with-sysroot="$SYSROOT" --prefix="$SYSROOT" &&
	make $PARALLEL &&
	make $PARALLEL install)
fi &&
exit 0 &&
if test ! -f "$SYSROOT"/$TARGET/include/winnt.h
then
	if test ! -d mingw-w64
	then
		git clone git://pacific.mpi-cbg.de/mingw-w64
	fi &&
	(mkdir -p build-headers &&
	 cd build-headers &&
	 ../mingw-w64/mingw-w64-headers/configure --build=x86_64-unknown-linux \
                --host=$TARGET --prefix="$SYSROOT" &&
	 make $PARALLEL install)
fi &&
if test ! -h "$SYSROOT"/mingw
then
	ln -s $TARGET "$SYSROOT"/mingw
fi &&
if test ! -h "$SYSROOT"/$TARGET/lib64
then
	ln -s lib "$SYSROOT"/$TARGET/lib64
fi &&
if test ! -f "$SYSROOT"/bin/$TARGET-gcc
then
	(if test ! -d gcc
	 then
		git clone git://repo.or.cz/gcc.git
	 fi &&
	 for d in gmp-4.3.2 mpfr-2.4.2 mpc-0.8.1
	 do
		 if test ! -d $d
		 then
			case $d in
			mpc*)
				f=$d.tar.gz &&
				taropt=xzvf
				;;
			*)
				f=$d.tar.bz2 &&
				taropt=xjvf
				;;
			esac &&
			if test ! -f $f
			then
				curl -O ftp://gcc.gnu.org/pub/gcc/infrastructure/$f
			fi &&
			tar $taropt $f &&
			(cd $d &&
			 CFLAGS="-I$SYSROOT/include" \
			 LDFLAGS="-L$SYSROOT/lib" \
			./configure --prefix="$SYSROOT" \
				--enable-static --disable-shared &&
			 make $PARALLEL install) || break
		fi || break
	 done &&
	 mkdir -p build-gcc &&
	 cd build-gcc &&
	 ../gcc/configure --target=$TARGET --enable-targets=all \
		--with-gmp="$SYSROOT" --with-mpfr="$SYSROOT" \
		--with-mpc="$SYSROOT" \
		--with-sysroot="$SYSROOT" --prefix="$SYSROOT" &&
	 make $PARALLEL all-gcc &&
	 make $PARALLEL install-gcc)
fi &&
export LD_LIBRARY_PATH="$SYSROOT/lib" &&
export PATH="$SYSROOT/bin:$PATH" &&
PARALLEL= &&
if test ! -f "$SYSROOT"/$TARGET/lib/crt2.o
then
	(mkdir -p build-crt &&
	 cd build-crt &&
	 CFLAGS="-I$SYSROOT/$TARGET/include" \
	 LDFLAGS="-L$SYSROOT/$TARGET/lib" \
	 ../mingw-w64/mingw-w64-crt/configure --host=$TARGET \
		--enable-lib32 \
		--with-sysroot="$SYSROOT" --prefix="$SYSROOT" &&
	 make $PARALLEL &&
	 make $PARALLEL install)
fi &&
if test ! -f "$SYSROOT"/lib/gcc/$TARGET/*/libgcc_eh.a
then
	(cd build-gcc &&
	 make $PARALLEL &&
	 make $PARALLEL install)
fi

