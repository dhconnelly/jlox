package dev.dhc.lox;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.dhc.lox.Token.Type;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class ScannerTest {
  private Scanner scanner(String text) {
    return new Scanner(new ByteArrayInputStream(text.getBytes(UTF_8)));
  }

  @Test
  void testEmpty() throws LoxError, IOException {
    final var s = scanner("");
    assertEquals(Type.EOF, s.peekToken(0).type());
    assertEquals(Type.EOF, s.peekToken(99).type());
    assertEquals(Type.EOF, s.nextToken().type());
  }

  @Test
  void testPeek() throws LoxError, IOException {
    final var s = scanner("()(");
    assertEquals(Type.LEFT_PAREN, s.peekToken(0).type());
    assertEquals(Type.LEFT_PAREN, s.peekToken(0).type());
    assertEquals(Type.RIGHT_PAREN, s.peekToken(1).type());
    assertEquals(Type.LEFT_PAREN, s.peekToken(2).type());
    assertEquals(Type.LEFT_PAREN, s.nextToken().type());
    assertEquals(Type.RIGHT_PAREN, s.peekToken(0).type());
    assertEquals(Type.LEFT_PAREN, s.peekToken(1).type());
    assertEquals(Type.EOF, s.peekToken(2).type());
    assertEquals(Type.RIGHT_PAREN, s.nextToken().type());
    assertEquals(Type.LEFT_PAREN, s.peekToken(0).type());
    assertEquals(Type.LEFT_PAREN, s.nextToken().type());
    assertEquals(Type.EOF, s.peekToken(0).type());
    assertEquals(Type.EOF, s.nextToken().type());
  }
}
