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

objdump -t fiji |
sed -n -e 's/.* \([^ ]*\)\@\@\(GLIBC_2\.[^0].*\)/GLIBC_COMPAT_SYMBOL(\1);/p' \
>> glibc-compat.h
