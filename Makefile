.PHONY: clean build test

all: clean build test

clean:
	mvn clean

build:
	mvn -q -Dmaven.test.skip=true package

test:
	mvn test
	cd craftinginterpreters; dart tool/bin/test.dart chap10_functions --interpreter ../lox
