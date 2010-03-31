all: run

run:
	sh Build.sh $(shell test -f make-targets && cat make-targets || echo run)

fiji: fiji.cxx
	sh Build.sh fiji

.PHONY: jars/fake.jar
jars/fake.jar:
	sh Build.sh $@

# MicroManager
mm:
	test -f micromanager1.1/build.sh || \
		(git submodule init micromanager1.1 && \
		 git submodule update micromanager1.1)
	export JAVA_LIB_DIR='$(JAVA_LIB_DIR)'; \
	export JAVA_HOME="$$(pwd)/$(JAVA_HOME)/.."; \
	export JAVAINC="-I$$JAVA_HOME/include -I$$JAVA_HOME/include/linux"; \
	cd micromanager1.1 && sh build.sh

# ------------------------------------------------------------------------

portable-app: Fiji.app
	for arch in linux linux-amd64 win32; do \
		case $$arch in win32) exe=.exe;; *) exe=;; esac; \
		cp precompiled/fiji-$$arch$$exe $</; \
		jdk=$$(git ls-tree --name-only origin/java/$$arch:); \
		jre=$$jdk/jre; \
		git archive --prefix=$</java/$$arch/$$jre/ \
				origin/java/$$arch:$$jre | \
			tar xvf -; \
	done

Fiji.app-%:
	ARCH=$$(echo $@ | sed "s/^Fiji.app-//"); \
	case $$ARCH in \
	$(ARCH)) \
		case $$ARCH in win*) EXE=.exe;; *) EXE=;; esac; \
		mkdir -p $@/$(JAVA_HOME) && \
		mkdir -p $@/images && \
		cp -R precompiled/fiji-$$ARCH$$EXE $@/fiji$$EXE && \
		cp -R plugins macros jars misc $@ && \
		REL_PATH=$$(echo $(JAVA_HOME) | sed "s|java/$(ARCH)/||") && \
		git archive --prefix=java/$(ARCH)/$$REL_PATH/ \
				origin/java/$(ARCH):$$REL_PATH | \
			(cd $@ && tar xf -) && \
		cp images/icon.png $@/images/ \
	;; \
	*) \
		$(MAKE) ARCH=$$ARCH $@ \
	;; \
	esac

fiji-%.tar.bz2: Fiji.app-%
	tar cf - $< | bzip2 -9 > $@

fiji-%.zip: Fiji.app-%
	zip -9r $@ $<
