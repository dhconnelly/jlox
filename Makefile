.PHONY: clean init build test suite

all: clean init build test suite

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

suite: build
	./overrides/apply_overrides.sh craftinginterpreters
	cd craftinginterpreters; dart tool/bin/test.dart chap11_resolving --interpreter ../lox
	cd craftinginterpreters; git checkout -f
