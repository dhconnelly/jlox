package dev.dhc.lox;

import dev.dhc.lox.Error.SyntaxError;
import dev.dhc.lox.Token.Literal;
import dev.dhc.lox.Token.NumberLiteral;
import dev.dhc.lox.Token.StringLiteral;
import dev.dhc.lox.Token.Type;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
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

  public Token nextToken() {
    if (lookahead.isEmpty()) scan();
    final var token = lookahead.pollFirst();
    return token != null ? token : eofToken();
  }

  // note: O(n)
  public Token peekToken(int n) {
    for (int i = lookahead.size(); i <= n; i++) scan();
    return lookahead.stream().skip(n).findFirst().orElseGet(this::eofToken);
  }

  private int peek(int n) {
    try {
      reader.mark(n + 1);
      reader.skip(n);
      int c = reader.read();
      reader.reset();
      return c;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean isEof() {
    return peek(0) == -1;
  }

  private char advance() {
    try {
      int c = reader.read();
      if (c == -1) error("Unexpected eof.");
      if (c == '\n') line++;
      current.append((char) c);
      return (char) c;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void eat(char want, String orError) {
    int got = peek(0);
    if (got != want) error(orError);
    advance();
  }

  private boolean maybeEat(Predicate<Character> p) {
    int c = peek(0);
    if (c != -1 && p.test((char) c)) {
      advance();
      return true;
    }
    return false;
  }

  private boolean maybeEat(char want) {
    return maybeEat(c -> c == want);
  }

  private void eatWhile(Predicate<Character> p) {
    while (maybeEat(p)) {
      // eat
    }
  }

  private void eatUntil(char want) {
    while (true) {
      int c = peek(0);
      if (c == -1 || c == want) break;
      advance();
    }
  }

  private void emit(Type type) {
    lookahead.add(new Token(line, type, current.toString(), Optional.empty()));
  }

  private void emit(Type type, Literal<?> literal) {
    lookahead.add(new Token(line, type, current.toString(), Optional.of(literal)));
  }

  private void error(String message) {
    throw new SyntaxError(line, message);
  }

  private boolean isDigit(int c) {
    return c >= '0' && c <= '9';
  }

  private boolean isAlpha(int c) {
    return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_';
  }

  private boolean isAlphaNumeric(int c) {
    return isDigit(c) || isAlpha(c);
  }

  private Type resolveType(String identifier) {
    return switch (identifier) {
      case "and" -> Type.AND;
      case "or" -> Type.OR;
      case "class" -> Type.CLASS;
      case "else" -> Type.ELSE;
      case "false" -> Type.FALSE;
      case "fun" -> Type.FUN;
      case "for" -> Type.FOR;
      case "if" -> Type.IF;
      case "nil" -> Type.NIL;
      case "print" -> Type.PRINT;
      case "return" -> Type.RETURN;
      case "super" -> Type.SUPER;
      case "this" -> Type.THIS;
      case "true" -> Type.TRUE;
      case "var" -> Type.VAR;
      case "while" -> Type.WHILE;
      default -> Type.IDENTIFIER;
    };
  }

  private void scan() {
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
          continue;
        }

        default -> {
          if (isDigit(c)) {
            eatWhile(this::isDigit);
            if (peek(0) == '.' && isDigit(peek(1))) {
              advance();
              eatWhile(this::isDigit);
            }
            emit(Type.NUMBER, new NumberLiteral(Double.parseDouble(current.toString())));
          } else if (isAlpha(c)) {
            eatWhile(this::isAlphaNumeric);
            emit(resolveType(current.toString()));
          } else {
            error(String.format("Unexpected character: %c", c));
          }
        }
      }
      break;
    }
  }
}
