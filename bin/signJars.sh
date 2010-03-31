#!/bin/sh

FIJIDIR="/var/www/webstart/fiji"

KEYSTORE=""
PASSWD="#6"
ALIAS=""
JARSIGNER="$FIJIDIR/java/linux-amd64/jdk1.6.0_04/bin/jarsigner"

# test if variables are set
if test -z $KEYSTORE || test -z $PASSWD || test -z $ALIAS; then
	echo "Parameters not set"
	exit
fi

# test if fijidir and jarsigner exists
if ! test -d $FIJIDIR; then
	echo "$FIJIDIR is not a directory"
	exit
fi

if ! test -f $JARSIGNER; then
	echo "$JARSIGNER does not exist"
	exit
fi

# sign jars in plugins folder
for i in $(find "$FIJIDIR/plugins/" -name \*.jar); do
	if ! $JARSIGNER -verify $i | grep 'jar verified'; then  
		echo "Signing $i"
		$JARSIGNER -keystore $KEYSTORE -storepass $PASSWD $i $ALIAS
	fi
done

# sign jars in jars folder
for i in $(find "$FIJIDIR/jars/" -name \*.jar); do
	$JARSIGNER -verify $FIJIDIR/jars/ij.jar | grep verified > /dev/null &&
	continue
	echo "Signing $i"
	$JARSIGNER -keystore $KEYSTORE -storepass $PASSWD $i $ALIAS
done
