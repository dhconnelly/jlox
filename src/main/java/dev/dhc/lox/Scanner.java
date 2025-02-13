package dev.dhc.lox;

import dev.dhc.lox.Token.Literal;
import dev.dhc.lox.Token.StringLiteral;
import dev.dhc.lox.Token.Type;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class Scanner {
  private final BufferedReader reader;
  private final Deque<Token> lookahead = new ArrayDeque<>();
  private int line = 1;
  private final StringBuilder current = new StringBuilder();

  public Scanner(InputStream in) {
    this.reader = new BufferedReader(new InputStreamReader(in));
  }

  private Token eofToken() {
    return new Token(line, Type.EOF, "", Optional.empty());
  }

  public Token nextToken() throws IOException, LoxError {
    if (lookahead.isEmpty()) scan();
    final var token = lookahead.pollFirst();
    return token != null ? token : eofToken();
  }

  // note: O(n)
  public Token peekToken(int n) throws IOException, LoxError {
    for (int i = lookahead.size(); i <= n; i++) scan();
    return lookahead.stream().skip(n).findFirst().orElseGet(this::eofToken);
  }

  private int peek(int n) throws IOException {
    reader.mark(n+1);
    reader.skip(n);
    int c = reader.read();
    reader.reset();
    return c;
  }

  private boolean isEof() throws IOException {
    return peek(0) == -1;
  }

  private char advance() throws IOException, LoxError {
    int c = reader.read();
    if (c == -1) error("Unexpected eof.");
    current.append((char)c);
    return (char)c;
  }

  private void eat(char want, String orError) throws LoxError, IOException {
    int got = peek(0);
    if (got != want) error(orError);
    advance();
  }

  private boolean maybeEat(char want) throws IOException, LoxError {
    if (peek(0) == want) {
      advance();
      return true;
    }
    return false;
  }

  private void eatUntil(char want) throws IOException, LoxError {
    while (true) {
      int c = peek(0);
      if (c == -1 || c == want) break;
      advance();
    }
  }

  private void emit(Type type) {
    lookahead.add(new Token(line, type, current.toString(), Optional.empty()));
  }

  private void emit(Type type, Literal literal) {
    lookahead.add(new Token(line, type, current.toString(), Optional.of(literal)));
  }

  private void error(String message) throws LoxError {
    throw new LoxError(line, message);
  }

  private void scan() throws IOException, LoxError {
    while (!isEof()) {
      current.setLength(0);
      char c = advance();
      switch (c) {
        case '(' -> emit(Type.LEFT_PAREN);
        case ')' -> emit(Type.RIGHT_PAREN);
        case '{' -> emit(Type.LEFT_BRACE);
        case '}' -> emit(Type.RIGHT_BRACE);
        case '.' -> emit(Type.DOT);
        case ',' -> emit(Type.COMMA);
        case ';' -> emit(Type.SEMICOLON);
        case '+' -> emit(Type.PLUS);
        case '-' -> emit(Type.MINUS);
        case '*' -> emit(Type.STAR);
        case '=' -> emit(maybeEat('=') ? Type.EQUAL_EQUAL : Type.EQUAL);
        case '!' -> emit(maybeEat('=') ? Type.BANG_EQUAL : Type.BANG);
        case '<' -> emit(maybeEat('=') ? Type.LESS_EQUAL : Type.LESS);
        case '>' -> emit(maybeEat('=') ? Type.GREATER_EQUAL : Type.GREATER);

        case '/' -> {
          if (maybeEat('/')) {
            eatUntil('\n');
            continue;
          } else {
            emit(Type.SLASH);
          }
        }

        case '"' -> {
          eatUntil('"');
          eat('"', "Unterminated string.");
          final var literal = new StringLiteral(current.substring(1, current.length()-1));
          emit(Type.STRING, literal);
        }

        case ' ', '\t', '\n' -> {
          if (c == '\n') line++;
          continue;
        }

        default -> error(String.format("Unexpected character: %c", c));
      }
      break;
    }
  }
}
