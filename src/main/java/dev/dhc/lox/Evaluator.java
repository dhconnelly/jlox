package dev.dhc.lox;

import dev.dhc.lox.AstNode.AssignExpr;
import dev.dhc.lox.AstNode.BinOp;
import dev.dhc.lox.AstNode.BinaryExpr;
import dev.dhc.lox.AstNode.BlockStmt;
import dev.dhc.lox.AstNode.BoolExpr;
import dev.dhc.lox.AstNode.Expr;
import dev.dhc.lox.AstNode.ExprStmt;
import dev.dhc.lox.AstNode.Grouping;
import dev.dhc.lox.AstNode.NilExpr;
import dev.dhc.lox.AstNode.NumExpr;
import dev.dhc.lox.AstNode.PrintStmt;
import dev.dhc.lox.AstNode.Program;
import dev.dhc.lox.AstNode.Stmt;
import dev.dhc.lox.AstNode.StrExpr;
import dev.dhc.lox.AstNode.UnaryExpr;
import dev.dhc.lox.AstNode.UnaryOp;
import dev.dhc.lox.AstNode.VarDecl;
import dev.dhc.lox.AstNode.VarExpr;
import dev.dhc.lox.LoxError.RuntimeError;
import dev.dhc.lox.Value.BoolValue;
import dev.dhc.lox.Value.NilValue;
import dev.dhc.lox.Value.NumValue;
import dev.dhc.lox.Value.StrValue;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;

public class Evaluator {
  private static final Value NIL = new NilValue();
  private final PrintStream out;
  private Environment env = new Environment();

  public Evaluator(PrintStream out) {
    this.out = out;
  }

  private boolean isTruthy(Expr e) {
    return switch (e) {
      case NilExpr _ -> false;
      case BoolExpr(_, boolean value) -> value;
      default -> true;
    };
  }

  private double asNumber(Expr e) {
    return switch (evaluate(e)) {
      case NumValue(double value) -> value;
      default -> throw new RuntimeError(e.line(), String.format("not a number: %s", e));
    };
  }

  private String asString(Expr e) {
    return switch (evaluate(e)) {
      case StrValue(String value) -> value;
      default -> throw new RuntimeError(e.line(), String.format("not a string: %s", e));
    };
  }

  public void run(Program program) {
    for (final var stmt : program.stmts()) {
      execute(stmt);
    }
  }

  private void executeBlock(List<Stmt> stmts, Environment env) {
    final var prev = this.env;
    try {
      this.env = env;
      for (var stmt : stmts) {
        execute(stmt);
      }
    } finally {
      this.env = prev;
    }
  }

  public void execute(Stmt stmt) {
    switch (stmt) {
      case ExprStmt(_, Expr e) -> evaluate(e);
      case PrintStmt(_, Expr e) -> out.println(evaluate(e));
      case VarDecl(_, String name, Optional<Expr> init) ->
          env.define(name, init.map(this::evaluate).orElse(NIL));
      case BlockStmt(_, List<Stmt> stmts) ->
          executeBlock(stmts, new Environment(env));
    }
  }

  public Value evaluate(Expr expr) {
    return switch (expr) {
      case BoolExpr(_, boolean value) -> new BoolValue(value);
      case StrExpr(_, String value) -> new StrValue(value);
      case NumExpr(_, double value) -> new NumValue(value);
      case NilExpr(_) -> new NilValue();
      case Grouping(_, Expr e) -> evaluate(e);
      case VarExpr(_, String name) -> env.get(name);
      case AssignExpr(_, String name, Expr e) -> env.assign(name, evaluate(e));
      case UnaryExpr(_, UnaryOp op, Expr e) -> switch (op) {
        case BANG -> new BoolValue(!isTruthy(e));
        case MINUS -> new NumValue(-asNumber(e));
      };
      case BinaryExpr(int line, Expr left, BinOp op, Expr right) -> switch (op) {
        case PLUS -> switch (evaluate(left)) {
          case NumValue lhs -> new NumValue(lhs.value() + asNumber(right));
          case StrValue lhs -> new StrValue(lhs.value() + asString(right));
          default -> throw new RuntimeError(line, "operands to + must be both string or number");
        };
        case MINUS -> new NumValue(asNumber(left) - asNumber(right));
        case SLASH -> new NumValue(asNumber(left) / asNumber(right));
        case STAR -> new NumValue(asNumber(left) * asNumber(right));
        case GREATER -> new BoolValue(asNumber(left) > asNumber(right));
        case GREATER_EQUAL -> new BoolValue(asNumber(left) >= asNumber(right));
        case LESS -> new BoolValue(asNumber(left) < asNumber(right));
        case LESS_EQUAL -> new BoolValue(asNumber(left) <= asNumber(right));
        case BANG_EQUAL -> new BoolValue(!evaluate(left).equals(evaluate(right)));
        case EQUAL_EQUAL -> new BoolValue(evaluate(left).equals(evaluate(right)));
      };
      case null -> throw new AssertionError("expr cannot be null");
    };
  }
}
