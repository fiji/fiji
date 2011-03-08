#!/bin/sh

cd "$(dirname "$0")"/..

symbols=
if test -f glibc-compat.h
then
	symbols="$(echo; grep ^GLIBC < glibc-compat.h)"
fi

# From http://www.trevorpounds.com/blog/?p=103

cat << EOF > glibc-compat.h
#ifdef __amd64__
   #define GLIBC_COMPAT_SYMBOL(FFF) __asm__(".symver " #FFF "," #FFF "@GLIBC_2.2.5")
#else
   #define GLIBC_COMPAT_SYMBOL(FFF) __asm__(".symver " #FFF "," #FFF "@GLIBC_2.0")
#endif /*__amd64__*/
$symbols
EOF

case "$(uname -m)" in
x86_64)
	version_regex='2\.2\.5'
	;;
*)
	version_regex='2\.0'
	;;
esac
objdump -t fiji |
grep -v -e "GLIBC_$version_regex" |
sed -n -e 's/.* \([^ ]*\)\@\@\(GLIBC_.*\)/GLIBC_COMPAT_SYMBOL(\1);/p' \
>> glibc-compat.h
