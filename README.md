## jlox

An implementation of jlox, the first variant of the Lox language described in the book
[Crafting Interpreters](https://www.craftinginterpreters.com/contents.html) by Robert Nystrom.

## status

done

- [x] codecrafters interpreter challenge (ch. 1-10 of the book)
- [x] full jlox compliance (ch. 11-13 of the book) with overrides applied (see below)

## overview

This implementation diverges from the one in the book in the following ways:

-   extensive use of records, sealed interfaces, and pattern matching instead of the visitor pattern
-   on-demand scanning
-   synchronization is not implemented, so only the first syntax error is reported. (this requires
    some overrides to the test suite, which are applied by the makefile)
-   uses a different driver and CLI so that it also satisfies the interface expected by the
    [CodeCrafters Interpreter challenge](https://app.codecrafters.io/courses/interpreter/overview)

## usage

just once, to fetch dependencies and so on:

    make all

to run the jlox test suite:

    make suite

there is a wrapper for the cli:

    ./lox           # run the repl
    ./lox FILE      # run a lox program stored in FILENAME

during development:

    make test       # build the project and run integration tests
    make suite      # run the test suite from the book repository
    make package    # build an executable jar at target/lox.jar
