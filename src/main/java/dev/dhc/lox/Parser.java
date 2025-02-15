package dev.dhc.lox;

import static dev.dhc.lox.Token.Type.BANG;
import static dev.dhc.lox.Token.Type.BANG_EQUAL;
import static dev.dhc.lox.Token.Type.EQUAL;
import static dev.dhc.lox.Token.Type.EQUAL_EQUAL;
import static dev.dhc.lox.Token.Type.GREATER;
import static dev.dhc.lox.Token.Type.GREATER_EQUAL;
import static dev.dhc.lox.Token.Type.IDENTIFIER;
import static dev.dhc.lox.Token.Type.LESS;
import static dev.dhc.lox.Token.Type.LESS_EQUAL;
import static dev.dhc.lox.Token.Type.MINUS;
import static dev.dhc.lox.Token.Type.PLUS;
import static dev.dhc.lox.Token.Type.RIGHT_PAREN;
import static dev.dhc.lox.Token.Type.SEMICOLON;
import static dev.dhc.lox.Token.Type.SLASH;
import static dev.dhc.lox.Token.Type.STAR;
import static dev.dhc.lox.Token.Type.VAR;

import dev.dhc.lox.AstNode.AssignExpr;
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
import dev.dhc.lox.LoxError.SyntaxError;
import dev.dhc.lox.Token.Type;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

public class Parser {
  private final Scanner scanner;

  public Parser(Scanner scanner) {
    this.scanner = scanner;
  }

  public boolean eof() throws IOException {
    return scanner.peekToken(0).type() == Type.EOF;
  }

  private Token next() throws IOException {
    return scanner.nextToken();
  }

  private Token peek() throws IOException {
    return scanner.peekToken(0);
  }

  private boolean peekIs(Type... types) throws IOException {
    final var tok = peek();
    for (Type type : types) {
      if (type == tok.type()) return true;
    }
    return false;
  }

  public Stmt stmt() throws IOException {
    final var tok = peek();
    return switch (tok.type()) {
      case PRINT -> {
        next();
        final var e = expr();
        eat(SEMICOLON, "Expected ; after expression");
        yield new PrintStmt(tok.line(), e);
      }
      default -> {
        final var e = expr();
        eat(SEMICOLON, "Expected ; after expression");
        yield new ExprStmt(tok.line(), e);
      }
    };
  }

  public Decl decl() throws IOException {
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
    return stmt();
  }

  public Program program() throws IOException {
    final var decls = new ArrayList<Decl>();
    while (!eof()) {
      decls.add(decl());
    }
    return new Program(decls);
  }

  private BinOp binOp(Token tok) {
    return switch (tok.type()) {
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

  public Expr expr() throws IOException {
    return assignment();
  }

  public Expr assignment() throws IOException {
    final var expr = equality();
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

  private Expr equality() throws IOException {
    var expr = comparison();
    while (peekIs(BANG_EQUAL, EQUAL_EQUAL)) {
      final var op = binOp(next());
      final var rhs = comparison();
      expr = new BinaryExpr(expr.line(), expr, op, rhs);
    }
    return expr;
  }

  private Expr comparison() throws IOException {
    var expr = term();
    while (peekIs(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      final var op = binOp(next());
      final var rhs = term();
      expr = new BinaryExpr(expr.line(), expr, op, rhs);
    }
    return expr;
  }

  private Expr term() throws IOException {
    var expr = factor();
    while (peekIs(MINUS, PLUS)) {
      final var op = binOp(next());
      final var rhs = factor();
      expr = new BinaryExpr(expr.line(), expr, op, rhs);
    }
    return expr;
  }

  private Expr factor() throws IOException {
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

  private Expr unary() throws IOException {
    if (peekIs(BANG, MINUS)) {
      final int line = peek().line();
      final var op = unaryOp(next());
      final var rhs = unary();
      return new UnaryExpr(line, op, rhs);
    }
    return primary();
  }

  private Token eat(Type type, String message) throws IOException {
    if (!peekIs(type)) throw new SyntaxError(peek().line(), message);
    return next();
  }

  private Expr primary() throws IOException {
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
