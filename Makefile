.PHONY: clean init build test ci

all: clean init build test ci

clean:
	cd craftinginterpreters; make clean
	mvn clean

init:
	cd craftinginterpreters; make get
	cd craftinginterpreters; make

build:
	mvn -q -Dmaven.test.skip=true package

test:
	mvn test

ci:
	./overrides/apply_overrides.sh craftinginterpreters
	cd craftinginterpreters; dart tool/bin/test.dart chap10_functions --interpreter ../lox
	cd craftinginterpreters; git checkout -f
