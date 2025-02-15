package dev.dhc.lox;

import dev.dhc.lox.AstNode.BinOp;
import dev.dhc.lox.AstNode.BinaryExpr;
import dev.dhc.lox.AstNode.BoolExpr;
import dev.dhc.lox.AstNode.Decl;
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
import java.util.Optional;

public class Evaluator {
  private final PrintStream out;
  private final Environment env = new Environment();
  private static final Value NIL = new NilValue();

  public Evaluator(PrintStream out) {
    this.out = out;
  }

  private boolean isTruthy(Expr e) {
    return switch (e) {
      case NilExpr nil -> false;
      case BoolExpr(int line, boolean value) -> value;
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
    for (final var decl : program.decls()) {
      execute(decl);
    }
  }

  public void execute(Decl decl) {
    switch (decl) {
      case ExprStmt(int line, Expr e) -> evaluate(e);
      case PrintStmt(int line, Expr e) -> out.println(evaluate(e));
      case VarDecl(int line, String name, Optional<Expr> init) ->
          env.defineGlobal(name, init.map(this::evaluate).orElse(NIL));
    }
  }

  public Value evaluate(Expr expr) {
    return switch (expr) {
      case BoolExpr(int line, boolean value) -> new BoolValue(value);
      case StrExpr(int line, String value) -> new StrValue(value);
      case NumExpr(int line, double value) -> new NumValue(value);
      case NilExpr(int line) -> new NilValue();
      case Grouping(int line, Expr e) -> evaluate(e);
      case VarExpr(int line, String name) -> env.get(name);

      case UnaryExpr(int line, UnaryOp op, Expr e) -> switch (op) {
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
