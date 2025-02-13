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

  public sealed interface Literal<Type> {
    default double asNumber() { throw new AssertionError("not a number"); }
    default String asString() { throw new AssertionError("not a string"); }
  }
  public record StringLiteral(String value) implements Literal<String> {
    @Override public String toString() { return value; }
    @Override public String asString() { return value; }
  }
  public record NumberLiteral(double value) implements Literal<Double> {
    @Override public String toString() { return Double.toString(value); }
    @Override public double asNumber() { return value; }
  }
}
