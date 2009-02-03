#  Copyrighs (c) 1997 Rainer Heintzmann
#  Dieses Makefile generiert View5D in two configurations

#	UNIX-makefile

S     = src
T	= view5DApplet view5DImageJ 

SRCS =	$(S)/View5D_.java 

JAVAC	= javac 
MV	= cp
RM	= rm -f 

#------------------------------------------------------------------

default: all

all	: $(T)

clean:
	 $(RM) $(T) *.class;

view5DApplet :  $(S)/View5D_.java 
	./javacApplet

view5DImageJ :  $(S)/View5D_.java 
	./javacImageJ


