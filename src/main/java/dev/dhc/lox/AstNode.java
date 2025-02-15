package dev.dhc.lox;

import java.util.List;

public sealed interface AstNode {
  int line();

  enum BinOp {
    BANG_EQUAL("!="), EQUAL_EQUAL("=="),
    PLUS("+"), MINUS("-"), SLASH("/"), STAR("*"),
    GREATER(">"), GREATER_EQUAL(">="),
    LESS("<"), LESS_EQUAL("<=");

    private final String s;
    BinOp(String s) { this.s = s; }
  }

  enum UnaryOp {
    BANG("!"), MINUS("-");

    private final String s;
    UnaryOp(String s) { this.s = s; }
  }

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
  record BinaryExpr(int line, Expr left, BinOp op, Expr right) implements Expr {
    @Override public String toString() {
      return String.format("(%s %s %s)", op.s, left, right);
    }
  }
  record UnaryExpr(int line, UnaryOp op, Expr expr) implements Expr {
    @Override public String toString() {
      return String.format("(%s %s)", op.s, expr);
    }
  }
  record Grouping(int line, Expr expr) implements Expr {
    @Override public String toString() {
      return String.format("(group %s)", expr);
    }
  }

  sealed interface Stmt extends AstNode {}
  record ExprStmt(int line, Expr expr) implements Stmt {}
  record PrintStmt(int line, Expr expr) implements Stmt {}

  record Program(List<Stmt> stmts) {}
}
