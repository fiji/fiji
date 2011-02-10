#!/bin/sh

cd "$(dirname "$0")"/..

# From http://www.trevorpounds.com/blog/?p=103

cat << EOF > glibc-compat.h
#ifdef __amd64__
   #define GLIBC_COMPAT_SYMBOL(FFF) __asm__(".symver " #FFF "," #FFF "@GLIBC_2.2.5")
#else
   #define GLIBC_COMPAT_SYMBOL(FFF) __asm__(".symver " #FFF "," #FFF "@GLIBC_2.0")
#endif /*__amd64__*/

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
