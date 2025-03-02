package dev.dhc.lox;

import static dev.dhc.lox.Token.Type.AND;
import static dev.dhc.lox.Token.Type.BANG;
import static dev.dhc.lox.Token.Type.BANG_EQUAL;
import static dev.dhc.lox.Token.Type.CLASS;
import static dev.dhc.lox.Token.Type.COMMA;
import static dev.dhc.lox.Token.Type.DOT;
import static dev.dhc.lox.Token.Type.ELSE;
import static dev.dhc.lox.Token.Type.EQUAL;
import static dev.dhc.lox.Token.Type.EQUAL_EQUAL;
import static dev.dhc.lox.Token.Type.FUN;
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
import dev.dhc.lox.AstNode.ThisExpr;
import dev.dhc.lox.AstNode.UnaryExpr;
import dev.dhc.lox.AstNode.UnaryOp;
import dev.dhc.lox.AstNode.VarDecl;
import dev.dhc.lox.AstNode.VarExpr;
import dev.dhc.lox.AstNode.WhileStmt;
import dev.dhc.lox.Error.SyntaxError;
import dev.dhc.lox.Token.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Parser {
  private static final int MAX_ARGS = 255;
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

  private Token eat(Type type, String message) {
    if (!peekIs(type)) throw new SyntaxError(peek(), message);
    return next();
  }

  private boolean peekIs(Type... types) {
    final var tok = peek();
    for (Type type : types) {
      if (type == tok.type()) return true;
    }
    return false;
  }

  private List<Stmt> block() {
    eat(LEFT_BRACE, "Expect '{' before block.");
    final var stmts = new ArrayList<Stmt>();
    while (!eof() && !peekIs(RIGHT_BRACE)) {
      stmts.add(stmt());
    }
    eat(RIGHT_BRACE, "Expect '}' after block.");
    return stmts;
  }

  private Stmt innerStmt() {
    final var tok = peek();
    return switch (tok.type()) {
      case LEFT_BRACE -> new BlockStmt(tok, block());

      case RETURN -> {
        next();
        final var e = peekIs(SEMICOLON) ? new NilExpr(tok) : expr();
        eat(SEMICOLON, "Expect ';' after return statement");
        yield new ReturnStmt(tok, e);
      }

      case PRINT -> {
        next();
        final var e = expr();
        eat(SEMICOLON, "Expected ; after expression");
        yield new PrintStmt(tok, e);
      }

      case IF -> {
        next();
        eat(LEFT_PAREN, "Expect '('");
        final var cond = expr();
        eat(RIGHT_PAREN, "Expect '('");
        final var conseq = innerStmt();
        var alt = Optional.<Stmt>empty();
        if (peekIs(ELSE)) {
          next();
          alt = Optional.of(innerStmt());
        }
        yield new IfElseStmt(tok, cond, conseq, alt);
      }

      case WHILE -> {
        next();
        eat(LEFT_PAREN, "Expect '('");
        final var cond = peekIs(RIGHT_PAREN) ? new BoolExpr(peek(), true) : expr();
        eat(RIGHT_PAREN, "Expect '('");
        final var body = innerStmt();
        yield new WhileStmt(tok, cond, body);
      }

      case FOR -> {
        next();
        eat(LEFT_PAREN, "Expect '('");
        final var init = peekIs(VAR) ? varDecl() : exprStmt();
        // already ate the first semicolon
        final var cond = peekIs(SEMICOLON) ? new BoolExpr(peek(), true) : expr();
        eat(SEMICOLON, "Expect ';'");
        final var iter = new ExprStmt(tok, peekIs(RIGHT_PAREN) ? new NilExpr(tok) : expr());
        eat(RIGHT_PAREN, "Expect '('");
        final var body = innerStmt();

        // desugar to while loop
        final var whileBody = new BlockStmt(tok, List.of(body, iter));
        final var whileLoop = new WhileStmt(tok, cond, whileBody);
        yield new BlockStmt(tok, List.of(init, whileLoop));
      }

      default -> exprStmt();
    };
  }

  private Stmt exprStmt() {
    final var e = peekIs(SEMICOLON) ? new NilExpr(peek()) : expr();
    eat(SEMICOLON, "Expected ; after expression");
    return new ExprStmt(e.tok(), e);
  }

  private Stmt varDecl() {
    final var tok = eat(VAR, "Expected 'var'");
    final var name = eat(IDENTIFIER, "Expect variable name.");
    var init = Optional.<Expr>empty();
    if (peekIs(EQUAL)) {
      next();
      init = Optional.of(expr());
    }
    eat(SEMICOLON, "Expected ; after variable declaration");
    return new VarDecl(tok, name, init);
  }

  private FunDecl function(String type) {
    final var name = eat(IDENTIFIER, String.format("Expect %s name", type));
    eat(LEFT_PAREN, String.format("Expect '(' after %s name", type));
    final var params = new ArrayList<Token>();
    while (!peekIs(RIGHT_PAREN)) {
      if (!params.isEmpty()) eat(COMMA, "Expect ')' after parameters.");
      final var param = eat(IDENTIFIER, "Expect parameter name");
      if (params.size() >= MAX_ARGS) {
        throw new SyntaxError(param, String.format("Can't have more than %d parameters.", MAX_ARGS));
      }
      params.add(param);
    }
    eat(RIGHT_PAREN, "Expect '(' after parameters");
    if (!peekIs(LEFT_BRACE)) {
      throw new SyntaxError(peek(), String.format("Expect '{' before %s body.", type));
    }
    final var body = block();
    return new FunDecl(name, name, params, body);
  }

  private Stmt funDecl() {
    eat(FUN, "Expect 'fun'");
    return function("function");
  }

  private Stmt classDecl() {
    final var tok = eat(CLASS, "Expect 'class'.");
    final var name = eat(IDENTIFIER, "Expect class name.");

    Optional<VarExpr> superclass = Optional.empty();
    if (peekIs(LESS)) {
      next();
      var superclassName = eat(IDENTIFIER, "Expect superclass name.");
      superclass = Optional.of(new VarExpr(superclassName, superclassName.cargo(), -1));
    }

    eat(LEFT_BRACE, "Expect '{' before class body.");
    final var methods = new ArrayList<FunDecl>();
    while (!eof() && !peekIs(RIGHT_BRACE)) {
      methods.add(function("method"));
    }
    eat(RIGHT_BRACE, "Expect '}' after class body");
    return new ClassDecl(tok, name, superclass, methods);
  }

  public Stmt stmt() {
    if (peekIs(CLASS)) return classDecl();
    if (peekIs(VAR)) return varDecl();
    if (peekIs(FUN)) return funDecl();
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
      default -> throw new SyntaxError(tok, "Expected binop.");
    };
  }

  public Expr expr() {
    return assignment();
  }

  private Expr assignment() {
    final var expr = or();
    if (peekIs(EQUAL)) {
      final var equal = next();
      final var binding = expr();
      if (expr instanceof VarExpr(Token tok, String name, _)) {
        return new AssignExpr(tok, name, -1, binding);
      } else if (expr instanceof GetExpr(Token tok, Expr object, Token name)) {
        return new SetExpr(tok, object, name, binding);
      } else {
        throw new SyntaxError(equal, "Invalid assignment target.");
      }
    }
    return expr;
  }

  private Expr or() {
    var expr = and();
    while (peekIs(OR)) {
      final var op = binOp(next());
      final var rhs = and();
      expr = new BinaryExpr(expr.tok(), expr, op, rhs);
    }
    return expr;
  }

  private Expr and() {
    var expr = equality();
    while (peekIs(AND)) {
      final var op = binOp(next());
      final var rhs = equality();
      expr = new BinaryExpr(expr.tok(), expr, op, rhs);
    }
    return expr;
  }

  private Expr equality() {
    var expr = comparison();
    while (peekIs(BANG_EQUAL, EQUAL_EQUAL)) {
      final var op = binOp(next());
      final var rhs = comparison();
      expr = new BinaryExpr(expr.tok(), expr, op, rhs);
    }
    return expr;
  }

  private Expr comparison() {
    var expr = term();
    while (peekIs(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      final var op = binOp(next());
      final var rhs = term();
      expr = new BinaryExpr(expr.tok(), expr, op, rhs);
    }
    return expr;
  }

  private Expr term() {
    var expr = factor();
    while (peekIs(MINUS, PLUS)) {
      final var op = binOp(next());
      final var rhs = factor();
      expr = new BinaryExpr(expr.tok(), expr, op, rhs);
    }
    return expr;
  }

  private Expr factor() {
    var expr = unary();
    while (peekIs(SLASH, STAR)) {
      final var op = binOp(next());
      final var rhs = unary();
      expr = new BinaryExpr(expr.tok(), expr, op, rhs);
    }
    return expr;
  }

  private UnaryOp unaryOp(Token tok) {
    return switch (tok.type()) {
      case BANG -> UnaryOp.BANG;
      case MINUS -> UnaryOp.MINUS;
      default -> throw new SyntaxError(tok, "Expected unary op.");
    };
  }

  private Expr unary() {
    if (peekIs(BANG, MINUS)) {
      final var tok = peek();
      final var op = unaryOp(next());
      final var rhs = unary();
      return new UnaryExpr(tok, op, rhs);
    }
    return call();
  }

  private Expr call() {
    var expr = primary();
    while (true) {
      if (peekIs(LEFT_PAREN)) {
        // handle call
        next();
        final var args = new ArrayList<Expr>();
        while (!peekIs(RIGHT_PAREN)) {
          if (!args.isEmpty()) {
            eat(COMMA, "Expect ',' after argument");
          }
          if (args.size() >= MAX_ARGS) {
            throw new SyntaxError(peek(), String.format("Can't have more than %d arguments.", MAX_ARGS));
          }
          args.add(expr());
        }
        eat(RIGHT_PAREN, "Expect ')' after arguments.");
        expr = new CallExpr(expr.tok(), expr, args);
      } else if (peekIs(DOT)) {
        // handle get
        next();
        var name = eat(IDENTIFIER, "Expect property name after '.'.");
        expr = new GetExpr(expr.tok(), expr, name);
      } else {
        // done
        break;
      }
    }
    return expr;
  }

  private Expr primary() {
    final var tok = peek();
    return switch (tok.type()) {
      case NIL -> new NilExpr(next());
      case TRUE -> new BoolExpr(next(), true);
      case FALSE -> new BoolExpr(next(), false);
      case NUMBER -> new NumExpr(next(), tok.literal().get().asNumber());
      case STRING -> new StrExpr(next(), tok.literal().get().asString());
      case IDENTIFIER -> new VarExpr(next(), tok.cargo(), -1);
      case THIS -> new ThisExpr(next(), -1);
      case LEFT_PAREN -> {
        next();
        final var expr = expr();
        eat(RIGHT_PAREN, "Expect ')' after expression.");
        yield new Grouping(tok, expr);
      }
      default -> throw new SyntaxError(tok, "Expect expression.");
    };
  }
}
