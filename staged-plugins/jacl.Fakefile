JAVAVERSION=1.5
all <- jacl.jar

JAVALOCK=tcl/lang/library/java/javalock.tcl
MAINCLASS(jacl.jar)=tcl.lang.Shell
jacl.jar <- src/jacl/**/* src/tcljava/**/*.java \
	$JAVALOCK[src/tcljava/$JAVALOCK]
