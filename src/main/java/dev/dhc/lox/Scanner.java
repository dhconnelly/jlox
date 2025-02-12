package dev.dhc.lox;

import dev.dhc.lox.Token.Type;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

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

  // note: returns nonsense if eof
  private char advance() throws IOException {
    char c = (char) reader.read();
    current.append(c);
    return c;
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
        case ' ', '\t' -> {}
        case '\n' -> line++;
        default -> error("Unexpected character.");
      }
    }
  }
}
