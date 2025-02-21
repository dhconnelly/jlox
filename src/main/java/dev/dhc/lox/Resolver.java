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
import dev.dhc.lox.AstNode.ReturnStmt;
import dev.dhc.lox.AstNode.Stmt;
import dev.dhc.lox.AstNode.StrExpr;
import dev.dhc.lox.AstNode.UnaryExpr;
import dev.dhc.lox.AstNode.UnaryOp;
import dev.dhc.lox.AstNode.VarDecl;
import dev.dhc.lox.AstNode.VarExpr;
import dev.dhc.lox.AstNode.WhileStmt;
import dev.dhc.lox.Error.SyntaxError;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

public class Resolver {
  // Stack of scopes where scope: name in scope -> fully initialized
  private final Stack<Map<String, Boolean>> scopes = new Stack<>();

  private enum FunctionType {
    NONE,
    FUNCTION
  }
  private FunctionType currentFunction = FunctionType.NONE;

  public Program resolve(Program program) {
    return new Program(program.stmts().stream().map(this::resolve).toList());
  }

  private void beginScope() {
    scopes.push(new HashMap<>());
  }

  private void endScope() {
    scopes.pop();
  }

  private void declare(Token name) {
    // globals are not resolved
    if (scopes.empty()) return;
    var scope = scopes.peek();
    if (scope.containsKey(name.cargo())) {
      throw new SyntaxError(name, "Already a variable with this name in this scope.");
    }
    scope.put(name.cargo(), false);
  }

  private void define(Token name) {
    // globals are not resolved
    if (scopes.empty()) return;
    scopes.peek().put(name.cargo(), true);
  }

  private int resolveLocal(Expr expr, String name) {
    for (int i = scopes.size()-1; i >= 0; i--) {
      if (scopes.get(i).containsKey(name)) {
        return scopes.size()-1-i;
      }
    }
    return -1;
  }

  public Expr resolve(Expr expr) {
    return switch (expr) {
      case VarExpr varExpr -> {
        if (!scopes.empty() && scopes.peek().get(varExpr.name()) == Boolean.FALSE) {
          throw new SyntaxError(varExpr.tok(), "Can't read local variable in its own initializer.");
        }
        int depth = resolveLocal(varExpr, varExpr.name());
        yield new VarExpr(varExpr.tok(), varExpr.name(), depth);
      }

      case AssignExpr assignExpr -> {
        var e = resolve(assignExpr.e());
        var depth = resolveLocal(assignExpr, assignExpr.name());
        yield new AssignExpr(assignExpr.tok(), assignExpr.name(), depth, e);
      }

      case BinaryExpr(Token tok, Expr left, BinOp op, Expr right) -> {
        var lhs = resolve(left);
        var rhs = resolve(right);
        yield new BinaryExpr(tok, lhs, op, rhs);
      }

      case CallExpr(Token tok, Expr callee, List<Expr> args) -> {
        var f = resolve(callee);
        var a = args.stream().map(this::resolve).toList();
        yield new CallExpr(tok, f, a);
      }

      case BoolExpr(_, _), NilExpr(_), NumExpr(_, _), StrExpr(_, _) -> expr;
      case Grouping(Token tok, Expr e) -> new Grouping(tok, resolve(e));
      case UnaryExpr(Token tok, UnaryOp op, Expr e) -> new UnaryExpr(tok, op, resolve(e));
    };
  }

  private List<Stmt> resolve(List<Stmt> stmts) {
    return stmts.stream().map(this::resolve).toList();
  }

  private List<Stmt> resolveFunction(List<Token> params, List<Stmt> body, FunctionType type) {
    var enclosing = currentFunction;
    currentFunction = type;

    beginScope();
    for (var param : params) {
      declare(param);
      define(param);
    }
    var body2 = resolve(body);
    endScope();

    currentFunction = enclosing;
    return body2;
  }

  public Stmt resolve(Stmt stmt) {
    return switch (stmt) {
      case BlockStmt(Token tok, List<Stmt> stmts) -> {
        beginScope();
        var stmts2 = resolve(stmts);
        endScope();
        yield new BlockStmt(tok, stmts2);
      }

      case FunDecl(Token tok, Token name, List<Token> params, List<Stmt> body) -> {
        declare(name);
        define(name);
        yield new FunDecl(tok, name, params, resolveFunction(params, body, FunctionType.FUNCTION));
      }

      case IfElseStmt(Token tok, Expr cond, Stmt conseq, Optional<Stmt> alt) -> {
        var cond2 = resolve(cond);
        var conseq2 = resolve(conseq);
        var alt2 = alt.map(this::resolve);
        yield new IfElseStmt(tok, cond2, conseq2, alt2);
      }

      case VarDecl(Token tok, Token name, Optional<Expr> init) -> {
        declare(name);
        var init2 = init.map(this::resolve);
        define(name);
        yield new VarDecl(tok, name, init2);
      }

      case WhileStmt(Token tok, Expr cond, Stmt body) -> {
        var cond2 = resolve(cond);
        var body2 = resolve(body);
        yield new WhileStmt(tok, cond2, body2);
      }

      case ExprStmt(Token tok, Expr expr) -> new ExprStmt(tok, resolve(expr));
      case PrintStmt(Token tok, Expr expr) -> new PrintStmt(tok, resolve(expr));
      case ReturnStmt(Token tok, Expr expr) -> {
        if (currentFunction == FunctionType.NONE) {
          throw new SyntaxError(tok, "Can't return from top-level code.");
        }
        yield new ReturnStmt(tok, resolve(expr));
      }
    };
  }
}
