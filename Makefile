.PHONY: clean build test

all: clean build test

clean:
	mvn clean

build:
	mvn -q -Dmaven.test.skip=true package

test:
	mvn test
	./overrides/apply_overrides.sh craftinginterpreters
	cd craftinginterpreters; make
	cd craftinginterpreters; dart tool/bin/test.dart chap10_functions --interpreter ../lox
	cd craftinginterpreters; git checkout -f
