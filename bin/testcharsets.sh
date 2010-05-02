#!/bin/sh

show_all=
test a-v = a"$1" && show_all=t

string="$(LC_ALL=C cat)"

LF='
'
all=
total=$(iconv -l | wc -l)
nr=0
for charset in $(iconv -l)
do
	nr=$(($nr+1))
	printf "(%d/%d) %-60s\r" $nr $total $charset >&2
	reencoded="$(LC_ALL=C echo "$string" |
		iconv -f $charset -t utf-8 2>/dev/null )" ||
	continue
	reencoded="$(echo "$reencoded" | tr -d '[]()$^&*?/\\')"
	charset="$(echo "$charset" | tr -d '/')"
	if echo "$ALL" | grep -e "^$reencoded " > /dev/null
	then
		ALL="$(echo "$ALL" |
			sed "s/$reencoded .*/&, $charset/")"
	else
		ALL="$ALL$LF$reencoded $charset"
	fi
done

if test -t 1
then
	echo "$ALL" | less -S
else
	echo "$ALL"
fi
