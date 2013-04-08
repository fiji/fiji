all: run

run:
	sh Build.sh $(shell test -f make-targets && cat make-targets || echo run)
