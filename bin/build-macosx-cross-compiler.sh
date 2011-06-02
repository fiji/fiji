#!/bin/sh

cd "$(dirname "$0")"

TARGET=i686-apple-darwin8
TARGET64=x86_64-apple-darwin8
TARGET_PPC=powerpc-apple-darwin8
SDK=MacOSX10.6u.sdk
SYSROOT="$(pwd -P)/mac-sysroot"
test -d "$SYSROOT" || mkdir -p "$SYSROOT"
PARALLEL=-j5

if test ! -f /usr/include/gnu/stubs-32.h
then
	cat >&2 << EOF
You do not have the 32-bit libc installed. Please do so now:

	sudo apt-get install libc6-dev-i386 ia32-libs
EOF
	exit 1
fi

if test ! -f /usr/lib32/libstdc++.so
then
	cat >&2 << EOF
Your 32-bit standard C++ library is not properly installed. Please call:

	sudo ln -s libstdc++.so.6 /usr/lib32/libstdc++.so
EOF
	exit 1
fi

flex --help > /dev/null 2>&1
if test $? = 127
then
	echo "Please install flex" >&2
	exit 1
fi

bison --help > /dev/null 2>&1
if test $? = 127
then
	echo "Please install bison" >&2
	exit 1
fi

if test ! -f Payload
then
	cat >&2 << EOF
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
		git clone git://fiji.sc/iphone-dev odcctools &&
		(cd odcctools && git checkout -t origin/fiji)
	fi &&
	mkdir -p build-odcctools &&
	(cd build-odcctools &&
	 CFLAGS="-m32" LDFLAGS="-m32 -L/usr/lib32 -L$SYSROOT/lib" \
	 ../odcctools/configure \
		--target=$TARGET --enable-ld64 \
		--enable-targets=$TARGET,$TARGET64,$TARGET_PPC \
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
if test ! -f "$SYSROOT/usr/i686-apple-darwin8/usr/include/CoreFoundation/CoreFoundation.h"
then
	(cd "$SYSROOT/usr/i686-apple-darwin8/usr/include" &&
	for headers in ../../System/Library/Frameworks/*.framework/Headers
	do
		target=${headers%.framework/Headers} &&
		target=${target##*/} &&
		ln -s $headers $target
	done)
fi &&
if test ! -f "$SYSROOT/usr/i686-apple-darwin8/usr/include/CarbonCore/CarbonCore.h"
then
	(cd "$SYSROOT/usr/i686-apple-darwin8/usr/include" &&
	for headers in ../../System/Library/Frameworks/CoreServices.framework/Versions/A/Frameworks/*.framework/Headers
	do
		target=${headers%.framework/Headers} &&
		target=${target##*/} &&
		ln -s $headers $target
	done)
fi &&
if test ! -f "$SYSROOT/usr/i686-apple-darwin8/usr/include/ATS/ATS.h"
then
	(cd "$SYSROOT/usr/i686-apple-darwin8/usr/include" &&
	for headers in ../../System/Library/Frameworks/ApplicationServices.framework/Versions/A/Frameworks/*.framework/Headers
	do
		target=${headers%.framework/Headers} &&
		target=${target##*/} &&
		ln -s $headers $target
	done)
fi &&
if test ! -e "$SYSROOT/usr/include"
then
	ln -s i686-apple-darwin8/usr/include "$SYSROOT"/usr/
fi &&
if test ! -e "$SYSROOT/usr/lib"
then
	ln -s i686-apple-darwin8/usr/lib "$SYSROOT"/usr/
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
diff --git a/gcc/c-incpath.c b/gcc/c-incpath.c
index 44b581d..43dc7ba 100644
--- a/gcc/c-incpath.c
+++ b/gcc/c-incpath.c
@@ -165,7 +165,16 @@ add_standard_paths (const char *sysroot, const char *iprefix,
 
 	  /* Should this directory start with the sysroot?  */
 	  if (sysroot && p->add_sysroot)
-	    str = concat (sysroot, p->fname, NULL);
+	    {
+	      if (p->component && p->fname[0] == DIR_SEPARATOR)
+		{
+		  str = xstrdup (p->fname);
+		}
+	      else
+		{
+		  str = concat (sysroot, p->fname, NULL);
+		}
+	    }
 	  else
 	    str = update_path (p->fname, p->component);
 
EOF
		 patch -p1 < build-cflags.patch)
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
if test ! -f "$SYSROOT/bin/$TARGET_PPC-gcc"
then
	if test ! -f "$SYSROOT/bin/$TARGET_PPC-ld64"
	then
		mkdir -p build-odcctools-ppc &&
		(cd build-odcctools-ppc &&
		 CFLAGS="-m32" LDFLAGS="-m32 -L$SYSROOT/lib" \
		 ../odcctools/configure \
			--target=$TARGET_PPC --enable-ld64 \
			--with-sysroot="$SYSROOT" --prefix="$SYSROOT" &&
		 make $PARALLEL &&
		 make $PARALLEL install)
	fi &&
	mkdir -p build-macgcc-ppc &&
	(cd build-macgcc-ppc &&
	 export PATH="$SYSROOT/bin:$PATH" &&
	 CFLAGS_FOR_BUILD="-m32" CFLAGS="-m32" \
	 LDFLAGS="-L$SYSROOT/lib" \
	 ../gcc-5664/configure \
		--target=$TARGET_PPC --enable-ld64 \
		--disable-checking --enable-languages=c,c++ \
		--with-as="$SYSROOT/bin/$TARGET_PPC-as" \
		--with-ld="$SYSROOT/bin/$TARGET_PPC-ld64" \
		--enable-static --enable-shared --disable-nls \
		--disable-multilib \
		--with-sysroot="$SYSROOT" --prefix="$SYSROOT" &&
	make &&
	make $PARALLEL install)
fi &&
if test ! -e "$SYSROOT/x86_64-apple-darwin8/include"
then
	ln -s ../usr/include "$SYSROOT"/x86_64-apple-darwin8/
fi &&
for d in Library System
do
	if test ! -h "$SYSROOT"/$d
	then
		ln -s usr/i686-apple-darwin8/$d "$SYSROOT"
	fi
done
if test ! -h "$SYSROOT"/usr/lib/libstdc++.dylib
then
	ln -s libstdc++.6.dylib "$SYSROOT"/usr/lib/libstdc++.dylib
fi
