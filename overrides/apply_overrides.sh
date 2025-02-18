#!/bin/sh

set -e

BOOK_DIR=$1
THIS_DIR=$(dirname "$@")
OVERRIDES=unexpected_character.lox for/statement_condition.lox for/statement_initializer.lox

for override in $OVERRIDES; do
  rm "$BOOK_DIR"/"$override";
  cp "$THIS_DIR"/"$override" "$BOOK_DIR"/"$override";
done
