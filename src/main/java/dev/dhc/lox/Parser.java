package dev.dhc.lox;

import static dev.dhc.lox.Token.Type.AND;
import static dev.dhc.lox.Token.Type.BANG;
import static dev.dhc.lox.Token.Type.BANG_EQUAL;
import static dev.dhc.lox.Token.Type.ELSE;
import static dev.dhc.lox.Token.Type.EQUAL;
import static dev.dhc.lox.Token.Type.EQUAL_EQUAL;
import static dev.dhc.lox.Token.Type.GREATER;
import static dev.dhc.lox.Token.Type.GREATER_EQUAL;
import static dev.dhc.lox.Token.Type.IDENTIFIER;
import static dev.dhc.lox.Token.Type.LEFT_BRACE;
import static dev.dhc.lox.Token.Type.LEFT_PAREN;
import static dev.dhc.lox.Token.Type.LESS;
import static dev.dhc.lox.Token.Type.LESS_EQUAL;
import static dev.dhc.lox.Token.Type.MINUS;
import static dev.dhc.lox.Token.Type.OR;
import static dev.dhc.lox.Token.Type.PLUS;
import static dev.dhc.lox.Token.Type.RIGHT_BRACE;
import static dev.dhc.lox.Token.Type.RIGHT_PAREN;
import static dev.dhc.lox.Token.Type.SEMICOLON;
import static dev.dhc.lox.Token.Type.SLASH;
import static dev.dhc.lox.Token.Type.STAR;
import static dev.dhc.lox.Token.Type.VAR;

import dev.dhc.lox.AstNode.AssignExpr;
import dev.dhc.lox.AstNode.BinOp;
import dev.dhc.lox.AstNode.BinaryExpr;
import dev.dhc.lox.AstNode.BlockStmt;
import dev.dhc.lox.AstNode.BoolExpr;
import dev.dhc.lox.AstNode.Expr;
import dev.dhc.lox.AstNode.ExprStmt;
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
import dev.dhc.lox.LoxError.SyntaxError;
import dev.dhc.lox.Token.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Parser {
  private final Scanner scanner;

  public Parser(Scanner scanner) {
    this.scanner = scanner;
  }

  public boolean eof() {
    return scanner.peekToken(0).type() == Type.EOF;
  }

  private Token next() {
    return scanner.nextToken();
  }

  private Token peek() {
    return scanner.peekToken(0);
  }

  private boolean peekIs(Type... types) {
    final var tok = peek();
    for (Type type : types) {
      if (type == tok.type()) return true;
    }
    return false;
  }

  private List<Stmt> block() {
    eat(LEFT_BRACE, "Expected '{'");
    final var stmts = new ArrayList<Stmt>();
    while (!eof() && !peekIs(RIGHT_BRACE)) {
      stmts.add(stmt());
    }
    eat(RIGHT_BRACE, "Expected '}'");
    return stmts;
  }

  private Stmt innerStmt() {
    final var tok = peek();
    return switch (tok.type()) {
      case LEFT_BRACE -> new BlockStmt(tok.line(), block());

      case PRINT -> {
        next();
        final var e = expr();
        eat(SEMICOLON, "Expected ; after expression");
        yield new PrintStmt(tok.line(), e);
      }

      case IF -> {
        next();
        eat(LEFT_PAREN, "Expect '('");
        var cond = expr();
        eat(RIGHT_PAREN, "Expect '('");
        var conseq = innerStmt();
        var alt = Optional.<Stmt>empty();
        if (peekIs(ELSE)) {
          next();
          alt = Optional.of(innerStmt());
        }
        yield new IfElseStmt(tok.line(), cond, conseq, alt);
      }

      case WHILE -> {
        next();
        eat(LEFT_PAREN, "Expect '('");
        var cond = expr();
        eat(RIGHT_PAREN, "Expect '('");
        var body = innerStmt();
        yield new WhileStmt(tok.line(), cond, body);
      }

      default -> {
        final var e = expr();
        eat(SEMICOLON, "Expected ; after expression");
        yield new ExprStmt(tok.line(), e);
      }
    };
  }

  public Stmt stmt() {
    int line = peek().line();
    if (peekIs(VAR)) {
      next();
      final var name = eat(IDENTIFIER, "Expected variable name").cargo();
      var init = Optional.<Expr>empty();
      if (peekIs(EQUAL)) {
        next();
        init = Optional.of(expr());
      }
      eat(SEMICOLON, "Expected ; after variable declaration");
      return new VarDecl(line, name, init);
    }
    return innerStmt();
  }

  public Program program() {
    final var stmts = new ArrayList<Stmt>();
    while (!eof()) {
      stmts.add(stmt());
    }
    return new Program(stmts);
  }

  private BinOp binOp(Token tok) {
    return switch (tok.type()) {
      case AND -> BinOp.AND;
      case OR -> BinOp.OR;
      case BANG_EQUAL -> BinOp.BANG_EQUAL;
      case EQUAL_EQUAL -> BinOp.EQUAL_EQUAL;
      case GREATER -> BinOp.GREATER;
      case GREATER_EQUAL -> BinOp.GREATER_EQUAL;
      case LESS -> BinOp.LESS;
      case LESS_EQUAL -> BinOp.LESS_EQUAL;
      case PLUS -> BinOp.PLUS;
      case MINUS -> BinOp.MINUS;
      case SLASH -> BinOp.SLASH;
      case STAR -> BinOp.STAR;
      default -> throw new SyntaxError(tok.line(), "Expected binop.");
    };
  }

  public Expr expr() {
    return assignment();
  }

  private Expr assignment() {
    final var expr = or();
    if (peekIs(EQUAL)) {
      next();
      final var binding = expr();
      if (expr instanceof VarExpr(int line, String name)) {
        return new AssignExpr(line, name, binding);
      } else {
        throw new SyntaxError(expr.line(), String.format("Invalid assignment target: %s", expr));
      }
    }
    return expr;
  }

  private Expr or() {
    var expr = and();
    while (peekIs(OR)) {
      final var op = binOp(next());
      final var rhs = and();
      expr = new BinaryExpr(expr.line(), expr, op, rhs);
    }
    return expr;
  }

  private Expr and() {
    var expr = equality();
    while (peekIs(AND)) {
      final var op = binOp(next());
      final var rhs = equality();
      expr = new BinaryExpr(expr.line(), expr, op, rhs);
    }
    return expr;
  }

  private Expr equality() {
    var expr = comparison();
    while (peekIs(BANG_EQUAL, EQUAL_EQUAL)) {
      final var op = binOp(next());
      final var rhs = comparison();
      expr = new BinaryExpr(expr.line(), expr, op, rhs);
    }
    return expr;
  }

  private Expr comparison() {
    var expr = term();
    while (peekIs(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      final var op = binOp(next());
      final var rhs = term();
      expr = new BinaryExpr(expr.line(), expr, op, rhs);
    }
    return expr;
  }

  private Expr term() {
    var expr = factor();
    while (peekIs(MINUS, PLUS)) {
      final var op = binOp(next());
      final var rhs = factor();
      expr = new BinaryExpr(expr.line(), expr, op, rhs);
    }
    return expr;
  }

  private Expr factor() {
    var expr = unary();
    while (peekIs(SLASH, STAR)) {
      final var op = binOp(next());
      final var rhs = unary();
      expr = new BinaryExpr(expr.line(), expr, op, rhs);
    }
    return expr;
  }

  private UnaryOp unaryOp(Token tok) {
    return switch (tok.type()) {
      case BANG -> UnaryOp.BANG;
      case MINUS -> UnaryOp.MINUS;
      default -> throw new SyntaxError(tok.line(), "Expected unary op.");
    };
  }

  private Expr unary() {
    if (peekIs(BANG, MINUS)) {
      final int line = peek().line();
      final var op = unaryOp(next());
      final var rhs = unary();
      return new UnaryExpr(line, op, rhs);
    }
    return primary();
  }

  private Token eat(Type type, String message) {
    if (!peekIs(type)) throw new SyntaxError(peek().line(), message);
    return next();
  }

  private Expr primary() {
    final var tok = peek();
    return switch (tok.type()) {
      case NIL -> new NilExpr(next().line());
      case TRUE -> new BoolExpr(next().line(), true);
      case FALSE -> new BoolExpr(next().line(), false);
      case NUMBER -> new NumExpr(next().line(), tok.literal().get().asNumber());
      case STRING -> new StrExpr(next().line(), tok.literal().get().asString());
      case IDENTIFIER -> new VarExpr(next().line(), tok.cargo());
      case LEFT_PAREN -> {
        next();
        final var expr = expr();
        eat(RIGHT_PAREN, "Expect ')' after expression.");
        yield new Grouping(tok.line(), expr);
      }
      default -> throw new SyntaxError(tok.line(), "Expect expression.");
    };
  }
}
