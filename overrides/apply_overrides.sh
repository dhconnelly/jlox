#!/bin/sh

set -e

BOOK_DIR=$1
if [ -z "$BOOK_DIR" ]; then
  echo "usage: apply_overrides BOOK_DIR";
  exit 1;
fi
THIS_DIR="$(dirname "$0")"
OVERRIDES="unexpected_character.lox for/statement_condition.lox for/statement_initializer.lox super/super_at_top_level.lox"

for override in $OVERRIDES; do
  rm "$BOOK_DIR"/test/"$override";
  cp "$THIS_DIR"/"$override" "$BOOK_DIR"/test/"$override";
done
