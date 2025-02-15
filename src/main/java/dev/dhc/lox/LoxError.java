package dev.dhc.lox;

public sealed abstract class LoxError extends RuntimeException {
  private final int code;

  public LoxError(int code, int line, String message) {
    super(String.format("[line %d] Error: %s", line, message));
    this.code = code;
  }

  public LoxError(int code, String message) {
    super(String.format("Error: %s", message));
    this.code = code;
  }

  public int code() { return code; }

  public static final class SyntaxError extends LoxError {
    public SyntaxError(int line, String message) {
      super(65, line, message);
    }
  }

  public static final class RuntimeError extends LoxError {
    public RuntimeError(int line, String message) {
      super(70, line, message);
    }
    public RuntimeError(String message) {
      super(70, message);
    }
  }
}
