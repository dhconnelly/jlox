package dev.dhc.lox;

import java.util.Optional;

public record Token(
    int line,
    Token.Type type,
    String cargo,
    Optional<Literal> literal
) {
  @Override
  public String toString() {
    return String.format("%s %s %s", type, cargo, literal.orElse(null));
  }

  public enum Type {
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    IDENTIFIER, STRING, NUMBER,

    AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,

    EOF
  }

  public sealed interface Literal {}
  public record StringLiteral(String value) implements Literal {
    @Override public String toString() { return value; }
  }
  public record NumberLiteral(double value) implements Literal {
    @Override public String toString() { return Double.toString(value); }
  }
}
