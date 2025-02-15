package dev.dhc.lox;

import dev.dhc.lox.AstNode.Expr;
import dev.dhc.lox.Value.NilValue;

public class Evaluator {
  public Value evaluate(Expr expr) {
    return new NilValue();
  }
}
