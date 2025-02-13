package dev.dhc.lox;

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

  private boolean peekIs(int... want) throws IOException {
    int c = peek(0);
    for (int wanted : want) {
      if (c == wanted) return true;
    }
    return false;
  }

  private char advance() throws IOException, LoxError {
    int c = reader.read();
    if (c == -1) error("Unexpected eof.");
    current.append((char)c);
    return (char)c;
  }

  private void eat(char want) throws LoxError, IOException {
    char got = advance();
    if (got != want) {
      error(String.format("Wanted %c, got %c.", want, got));
    }
  }

  private boolean maybeEat(char want) throws IOException, LoxError {
    if (peek(0) == want) {
      eat(want);
      return true;
    }
    return false;
  }

  private void emit(Type type) {
    lookahead.add(new Token(line, type, current.toString(), Optional.empty()));
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

        case '=' -> {
          if (maybeEat('=')) {
            emit(Type.EQUAL_EQUAL);
          } else {
            emit(Type.EQUAL);
          }
        }
        case '!' -> {
          if (maybeEat('=')) {
            emit(Type.BANG_EQUAL);
          } else {
            emit(Type.BANG);
          }
        }
        case '<' -> {
          if (maybeEat('=')) {
            emit(Type.LESS_EQUAL);
          } else {
            emit(Type.LESS);
          }
        }
        case '>' -> {
          if (maybeEat('=')) {
            emit(Type.GREATER_EQUAL);
          } else {
            emit(Type.GREATER);
          }
        }
        case '/' -> {
          if (maybeEat('/')) {
            while (!peekIs(-1, '\n')) {
              advance();
            }
            continue;
          } else {
            emit(Type.SLASH);
          }
        }
        /*
        case '"' -> {
          while (!peekIs(-1, '"')) {
            advance();
          }
          eat('"');
          emit(Type.STRING);
        }
         */
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
