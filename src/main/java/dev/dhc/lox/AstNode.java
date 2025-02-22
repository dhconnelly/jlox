package dev.dhc.lox;

import java.util.List;
import java.util.Optional;

public sealed interface AstNode {
  Token tok();

  enum BinOp {
    BANG_EQUAL("!="), EQUAL_EQUAL("=="),
    PLUS("+"), MINUS("-"), SLASH("/"), STAR("*"),
    GREATER(">"), GREATER_EQUAL(">="),
    LESS("<"), LESS_EQUAL("<="),
    AND("and"), OR("or");

    private final String s;
    BinOp(String s) { this.s = s; }
  }

  enum UnaryOp {
    BANG("!"), MINUS("-");

    private final String s;
    UnaryOp(String s) { this.s = s; }
  }

  sealed interface Expr extends AstNode {
    Token tok();
  }
  record NilExpr(Token tok) implements Expr {
    @Override public String toString() { return "nil"; }
  }
  record BoolExpr(Token tok, boolean value) implements Expr {
    @Override public String toString() { return Boolean.toString(value); }
  }
  record NumExpr(Token tok, double value) implements Expr {
    @Override public String toString() { return Double.toString(value); }
  }
  record StrExpr(Token tok, String value) implements Expr {
    @Override public String toString() { return value; }
  }
  record BinaryExpr(Token tok, Expr left, BinOp op, Expr right) implements Expr {
    @Override public String toString() {
      return String.format("(%s %s %s)", op.s, left, right);
    }
  }
  record UnaryExpr(Token tok, UnaryOp op, Expr expr) implements Expr {
    @Override public String toString() {
      return String.format("(%s %s)", op.s, expr);
    }
  }
  record Grouping(Token tok, Expr expr) implements Expr {
    @Override public String toString() {
      return String.format("(group %s)", expr);
    }
  }
  record VarExpr(Token tok, String name, int scopeDepth) implements Expr {
    @Override public String toString() {
      return name;
    }
  }
  record AssignExpr(Token tok, String name, int scopeDepth, Expr e) implements Expr {
    @Override public String toString() {
      return String.format("(assign %s %s)", name, e);
    }
  }
  record CallExpr(Token tok, Expr callee, List<Expr> arguments) implements Expr {
    @Override public String toString() {
      return String.format("(call %s %s)", callee, arguments);
    }
  }

  sealed interface Stmt extends AstNode {}
  record VarDecl(Token tok, String name, Optional<Expr> init) implements Stmt {}
  record FunDecl(Token tok, String name, List<String> params, List<Stmt> body) implements Stmt {}
  record ExprStmt(Token tok, Expr expr) implements Stmt {}
  record PrintStmt(Token tok, Expr expr) implements Stmt {}
  record BlockStmt(Token tok, List<Stmt> stmts) implements Stmt {}
  record IfElseStmt(Token tok, Expr cond, Stmt conseq, Optional<Stmt> alt) implements Stmt {}
  record WhileStmt(Token tok, Expr cond, Stmt body) implements Stmt {}
  record ReturnStmt(Token tok, Expr expr) implements Stmt {}

  record Program(List<Stmt> stmts) {}
}
