package dev.dhc.lox;

import dev.dhc.lox.AstNode.AssignExpr;
import dev.dhc.lox.AstNode.BinOp;
import dev.dhc.lox.AstNode.BinaryExpr;
import dev.dhc.lox.AstNode.BlockStmt;
import dev.dhc.lox.AstNode.BoolExpr;
import dev.dhc.lox.AstNode.CallExpr;
import dev.dhc.lox.AstNode.Expr;
import dev.dhc.lox.AstNode.ExprStmt;
import dev.dhc.lox.AstNode.FunDecl;
import dev.dhc.lox.AstNode.Grouping;
import dev.dhc.lox.AstNode.IfElseStmt;
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
import dev.dhc.lox.AstNode.WhileStmt;
import dev.dhc.lox.Error.RuntimeError;
import dev.dhc.lox.Value.BoolValue;
import dev.dhc.lox.Value.LoxCallable;
import dev.dhc.lox.Value.LoxFunction;
import dev.dhc.lox.Value.LoxNativeFunction;
import dev.dhc.lox.Value.NilValue;
import dev.dhc.lox.Value.NumValue;
import dev.dhc.lox.Value.StrValue;
import dev.dhc.lox.Value.Type;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;

public class Evaluator {
  private static final Value NIL = new NilValue();
  private final PrintStream out;
  private final Environment globals = new Environment();
  private Environment env = globals;

  public Evaluator(PrintStream out) {
    this.out = out;
    globals.define(
        "clock",
        new LoxNativeFunction(0, (_, _) ->
            new NumValue((double) System.currentTimeMillis() / 1000.0)));
  }

  private RuntimeError typeError(Expr e, Type want, Value got) {
    return new RuntimeError(e.line(), String.format("%s: want %s, got %s", e, want, got.type()));
  }

  private RuntimeError arityError(int line, LoxCallable f, List<Value> args) {
    return new RuntimeError(line, String.format("expected %d arguments, got %d", f.arity(), args.size()));
  }

  private LoxCallable asCallable(Expr e) {
    return switch (evaluate(e)) {
      case LoxCallable c -> c;
      case Value v -> throw typeError(e, Type.CALLABLE, v);
    };
  }

  private double asNumber(Expr e) {
    return switch (evaluate(e)) {
      case NumValue(double value) -> value;
      case Value v -> throw typeError(e, Type.NUM, v);
    };
  }

  private String asString(Expr e) {
    return switch (evaluate(e)) {
      case StrValue(String value) -> value;
      case Value v -> throw typeError(e, Type.STR, v);
    };
  }

  private boolean isTruthy(Value v) {
    return switch (v) {
      case NilValue() -> false;
      case BoolValue(boolean value) -> value;
      default -> true;
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

  public Value call(LoxFunction f, List<Value> args) {
    final var env = new Environment(globals);
    for (int i = 0; i < args.size(); i++) {
      env.define(f.params().get(i), args.get(i));
    }
    executeBlock(f.body(), env);
    return NIL;
  }

  public void execute(Stmt stmt) {
    switch (stmt) {
      case ExprStmt(_, Expr e) -> evaluate(e);
      case PrintStmt(_, Expr e) -> out.println(evaluate(e));
      case VarDecl(_, String name, Optional<Expr> init) ->
          env.define(name, init.map(this::evaluate).orElse(NIL));
      case BlockStmt(_, List<Stmt> stmts) ->
          executeBlock(stmts, new Environment(env));
      case IfElseStmt(_, Expr cond, Stmt conseq, Optional<Stmt> alt) -> {
        if (isTruthy(evaluate(cond))) execute(conseq);
        else alt.ifPresent(this::execute);
      }
      case WhileStmt(_, Expr cond, Stmt body) -> {
        while (isTruthy(evaluate(cond))) execute(body);
      }
      case FunDecl(_, String name, List<String> params, List<Stmt> body) -> {
        final var f = new LoxFunction(name, params, body);
        env.define(name, f);
      }
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
        case BANG -> new BoolValue(!isTruthy(evaluate(e)));
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
        case AND -> {
          final var lhs = evaluate(left);
          yield !isTruthy(lhs) ? lhs : evaluate(right);
        }
        case OR -> {
          final var lhs = evaluate(left);
          yield isTruthy(lhs) ? lhs : evaluate(right);
        }
      };
      case CallExpr(_, Expr callee, List<Expr> args) -> {
        final var f = asCallable(callee);
        final var a = args.stream().map(this::evaluate).toList();
        if (f.arity() != a.size()) throw arityError(callee.line(), f, a);
        yield f.call(this, a);
      }
    };
  }
}
