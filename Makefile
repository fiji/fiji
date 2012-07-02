all: run

run:
	sh Build.sh $(shell test -f make-targets && cat make-targets || echo run)

ImageJ: ImageJ.c
	sh Build.sh ImageJ

.PHONY: jars/fake.jar
jars/fake.jar:
	sh Build.sh $@
