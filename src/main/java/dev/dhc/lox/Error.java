package dev.dhc.lox;

public sealed abstract class Error extends RuntimeException {
  private final Status code;

  private Error(Status code, String message) {
    super(message);
    this.code = code;
  }

  public Status code() { return code; }

  public static final class SyntaxError extends Error {
    public SyntaxError(Token tok, String message) {
      super(Status.SYNTAX_ERROR, switch (tok.type()) {
        case Token.Type.EOF -> String.format("[line %d] Error at end: %s", tok.line(), message);
        default -> String.format("[line %d] Error at '%s': %s", tok.line(), tok.cargo(), message);
      });
    }

    public SyntaxError(int line, String message) {
      super(Status.SYNTAX_ERROR, String.format("[line %d] Error: %s", line, message));
    }
  }

  public static final class RuntimeError extends Error {
    public RuntimeError(int line, String message) {
      super(Status.RUNTIME_ERROR, String.format("%s\n[line %d]", message, line));
    }
  }
}
