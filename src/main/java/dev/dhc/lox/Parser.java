package dev.dhc.lox;

import dev.dhc.lox.AstNode.BoolExpr;
import dev.dhc.lox.AstNode.Expr;
import dev.dhc.lox.AstNode.NilExpr;
import dev.dhc.lox.AstNode.NumExpr;
import dev.dhc.lox.AstNode.StrExpr;
import dev.dhc.lox.Token.Type;
import java.io.IOException;

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

  public Expr expr() throws LoxError, IOException {
    final var tok = scanner.peekToken(0);
    return switch (tok.type()) {
      case NIL -> new NilExpr(next().line());
      case TRUE -> new BoolExpr(next().line(), true);
      case FALSE -> new BoolExpr(next().line(), false);
      case NUMBER -> new NumExpr(next().line(), tok.literal().get().asNumber());
      case STRING -> new StrExpr(next().line(), tok.literal().get().asString());
      default -> throw new LoxError(tok.line(), "Expect expression.");
    };
  }
}
