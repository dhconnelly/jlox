package dev.dhc.lox;

import dev.dhc.lox.AstNode.AssignExpr;
import dev.dhc.lox.AstNode.BinOp;
import dev.dhc.lox.AstNode.BinaryExpr;
import dev.dhc.lox.AstNode.BlockStmt;
import dev.dhc.lox.AstNode.BoolExpr;
import dev.dhc.lox.AstNode.CallExpr;
import dev.dhc.lox.AstNode.ClassDecl;
import dev.dhc.lox.AstNode.Expr;
import dev.dhc.lox.AstNode.ExprStmt;
import dev.dhc.lox.AstNode.FunDecl;
import dev.dhc.lox.AstNode.GetExpr;
import dev.dhc.lox.AstNode.Grouping;
import dev.dhc.lox.AstNode.IfElseStmt;
import dev.dhc.lox.AstNode.NilExpr;
import dev.dhc.lox.AstNode.NumExpr;
import dev.dhc.lox.AstNode.PrintStmt;
import dev.dhc.lox.AstNode.Program;
import dev.dhc.lox.AstNode.ReturnStmt;
import dev.dhc.lox.AstNode.SetExpr;
import dev.dhc.lox.AstNode.Stmt;
import dev.dhc.lox.AstNode.StrExpr;
import dev.dhc.lox.AstNode.SuperExpr;
import dev.dhc.lox.AstNode.ThisExpr;
import dev.dhc.lox.AstNode.UnaryExpr;
import dev.dhc.lox.AstNode.UnaryOp;
import dev.dhc.lox.AstNode.VarDecl;
import dev.dhc.lox.AstNode.VarExpr;
import dev.dhc.lox.AstNode.WhileStmt;
import dev.dhc.lox.Error.RuntimeError;
import dev.dhc.lox.Value.BoolValue;
import dev.dhc.lox.Value.FunctionType;
import dev.dhc.lox.Value.LoxCallable;
import dev.dhc.lox.Value.LoxClass;
import dev.dhc.lox.Value.LoxFunction;
import dev.dhc.lox.Value.LoxInstance;
import dev.dhc.lox.Value.LoxNativeFunction;
import dev.dhc.lox.Value.NilValue;
import dev.dhc.lox.Value.NumValue;
import dev.dhc.lox.Value.StrValue;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

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

  private RuntimeError error(Token tok, String format, Object... args) {
    return new RuntimeError(tok.line(), String.format(format, args));
  }

  private record Pair<T, U>(T t, U u) {
    <V> V reduce(BiFunction<T, U, V> f) {
      return f.apply(t, u);
    }
  }

  private Pair<Value, Value> evaluate(Expr lhs, Expr rhs) {
    return new Pair<>(evaluate(lhs), evaluate(rhs));
  }

  private LoxCallable asCallable(Expr e) {
    return switch (evaluate(e)) {
      case LoxCallable c -> c;
      case Value ignored -> throw error(e.tok(), "Can only call functions and classes.");
    };
  }

  private double asNumber(Expr e) {
    return switch (evaluate(e)) {
      case NumValue(double value) -> value;
      case Value ignored -> throw error(e.tok(), "Operand must be a number.");
    };
  }

  private Pair<Double, Double> asNumbers(Expr lhs, Expr rhs) {
    return switch (evaluate(lhs, rhs)) {
      case Pair(NumValue left, NumValue right) -> new Pair<>(left.value(), right.value());
      default -> throw error(lhs.tok(), "Operands must be numbers.");
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

  public Value call(LoxFunction f, Environment closure, List<Value> args) {
    final var env = new Environment(closure);
    for (int i = 0; i < args.size(); i++) {
      env.define(f.params().get(i), args.get(i));
    }
    try {
      executeBlock(f.body(), env);
    } catch (Return retvrn) {
      return retvrn.result;
    }
    return NIL;
  }

  private static class Return extends RuntimeException {
    final Value result;
    Return(Value result) {
      super(null, null, false, false);
      this.result = result;
    }
  }

  public void execute(Stmt stmt) {
    switch (stmt) {
      case ExprStmt(_, Expr e) -> evaluate(e);
      case PrintStmt(_, Expr e) -> out.println(evaluate(e));
      case VarDecl(_, var name, Optional<Expr> init) ->
          env.define(name.cargo(), init.map(this::evaluate).orElse(NIL));
      case BlockStmt(_, List<Stmt> stmts) ->
          executeBlock(stmts, new Environment(env));
      case IfElseStmt(_, Expr cond, Stmt conseq, Optional<Stmt> alt) -> {
        if (isTruthy(evaluate(cond))) execute(conseq);
        else alt.ifPresent(this::execute);
      }
      case WhileStmt(_, Expr cond, Stmt body) -> {
        while (isTruthy(evaluate(cond))) execute(body);
      }
      case FunDecl(_, var name, var params, List<Stmt> body) -> {
        final var f = new LoxFunction(name.cargo(), env, cargo(params), body, FunctionType.FUNCTION);
        env.define(name.cargo(), f);
      }
      case ReturnStmt(_, Expr result) -> throw new Return(evaluate(result));
      case ClassDecl(_, Token className, Optional<VarExpr> superclassName, List<FunDecl> methodDecls) -> {
        var superclassE = superclassName.map(this::evaluate);
        if (superclassE.map(sc -> !(sc instanceof LoxClass)).orElse(false)) {
          throw error(className, "Superclass must be a class.");
        }
        var superclass = superclassE.map(sc -> (LoxClass) sc);
        env.define(className.cargo(), null);
        var prev = env;
        superclass.ifPresent(sc -> {
          this.env = new Environment(prev);
          this.env.define("super", sc);
        });
        var methods = methodDecls.stream()
            .map(methodDecl -> methodFunction(env, methodDecl))
            .collect(Collectors.toMap(LoxFunction::name, id -> id));
        var klass = new LoxClass(className.cargo(), superclass, methods);
        superclass.ifPresent(_ -> this.env = prev);
        env.assign(className.cargo(), klass);
      }
    }
  }

  private static LoxFunction methodFunction(Environment env, FunDecl method) {
    return new LoxFunction(
        method.name().cargo(), env, cargo(method.params()), method.body(),
        method.name().cargo().equals("init") ? FunctionType.INITIALIZER : FunctionType.FUNCTION);
  }

  private static List<String> cargo(List<Token> tokens) {
    return tokens.stream().map(Token::cargo).toList();
  }

  private Error undefined(Token ident) {
    return error(ident, "Undefined variable '%s'.", ident.cargo());
  }

  private Value lookup(Token at, int depth, String name) {
    return depth >= 0
        ? env.getAt(depth, name)
        : globals.get(name).orElseThrow(() -> undefined(at));
  }

  private Value assign(AssignExpr varExpr, String name, Value value) {
    return varExpr.scopeDepth() >= 0
        ? env.assignAt(varExpr.scopeDepth(), name, value)
        : globals.assign(name, value).orElseThrow(() -> undefined(varExpr.tok()));
  }

  public Value evaluate(Expr expr) {
    return switch (expr) {
      case BoolExpr(_, boolean value) -> new BoolValue(value);
      case StrExpr(_, String value) -> new StrValue(value);
      case NumExpr(_, double value) -> new NumValue(value);
      case NilExpr(_) -> new NilValue();
      case Grouping(_, Expr e) -> evaluate(e);
      case VarExpr e -> lookup(e.tok(), e.scopeDepth(), e.tok().cargo());
      case AssignExpr e -> assign(e, e.name(), evaluate(e.e()));
      case UnaryExpr(_, UnaryOp op, Expr e) -> switch (op) {
        case BANG -> new BoolValue(!isTruthy(evaluate(e)));
        case MINUS -> new NumValue(-asNumber(e));
      };
      case BinaryExpr(Token tok, Expr left, BinOp op, Expr right) -> switch (op) {
        case PLUS -> switch (evaluate(left, right)) {
          case Pair(NumValue lhs, NumValue rhs) -> new NumValue(lhs.value() + rhs.value());
          case Pair(StrValue lhs, StrValue rhs) -> new StrValue(lhs.value() + rhs.value());
          default -> throw error(tok, "Operands must be two numbers or two strings.");
        };
        case MINUS -> asNumbers(left, right).reduce((lhs, rhs) -> new NumValue(lhs - rhs));
        case SLASH -> asNumbers(left, right).reduce((lhs, rhs) -> new NumValue(lhs / rhs));
        case STAR -> asNumbers(left, right).reduce((lhs, rhs) -> new NumValue(lhs * rhs));
        case GREATER -> asNumbers(left, right).reduce((lhs, rhs) -> new BoolValue(lhs > rhs));
        case GREATER_EQUAL -> asNumbers(left, right).reduce((lhs, rhs) -> new BoolValue(lhs >= rhs));
        case LESS -> asNumbers(left, right).reduce((lhs, rhs) -> new BoolValue(lhs < rhs));
        case LESS_EQUAL -> asNumbers(left, right).reduce((lhs, rhs) -> new BoolValue(lhs <= rhs));
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
      case CallExpr(Token tok, Expr callee, List<Expr> args) -> {
        final var f = asCallable(callee);
        final var a = args.stream().map(this::evaluate).toList();
        if (f.arity() != a.size()) {
          throw error(tok, "Expected %d arguments but got %d.", f.arity(), a.size());
        }
        yield f.call(this, a);
      }
      case GetExpr(Token tok, Expr object, Token name) -> {
        final var o = evaluate(object);
        if (o instanceof LoxInstance instance) {
          yield instance.get(name);
        }
        throw error(tok, "Only instances have properties.");
      }
      case SetExpr(Token tok, Expr objectExpr, Token name, Expr valueExpr) -> {
        final var object = evaluate(objectExpr);
        if (object instanceof LoxInstance instance) {
          final var value = evaluate(valueExpr);
          instance.set(name, value);
          yield value;
        }
        throw error(tok, "Only instances have fields.");
      }
      case ThisExpr(Token tok, int depth) -> lookup(tok, depth, "this");
      case SuperExpr(Token tok, Token methodName, int depth) -> {
        var superclass = (LoxClass) lookup(tok, depth, "super");
        var instance = (LoxInstance) lookup(tok, depth-1, "this");
        var name = methodName.cargo();
        var method = superclass.findMethod(name);
        yield method
            .orElseThrow(() -> error(tok, String.format("Undefined property '%s'.", name)))
            .bind(instance);
      }
    };
  }
}
