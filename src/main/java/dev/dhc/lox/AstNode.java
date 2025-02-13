package dev.dhc.lox;

public interface AstNode {
  int line();

  sealed interface Expr extends AstNode {}
  record NilExpr(int line) implements Expr {
    @Override public String toString() { return "nil"; }
  }
  record BoolExpr(int line, boolean value) implements Expr {
    @Override public String toString() { return Boolean.toString(value); }
  }
  record NumExpr(int line, double value) implements Expr {
    @Override public String toString() { return Double.toString(value); }
  }
  record StrExpr(int line, String value) implements Expr {
    @Override public String toString() { return value; }
  }
}
