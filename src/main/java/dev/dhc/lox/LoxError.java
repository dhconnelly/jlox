package dev.dhc.lox;

import java.io.IOException;

public sealed abstract class LoxError extends RuntimeException {
  private final Status code;

  public LoxError(Status code, int line, String message) {
    super(String.format("[line %d] Error: %s", line, message));
    this.code = code;
  }

  public LoxError(Status code, Throwable cause) {
    super(cause);
    this.code = code;
  }

  public LoxError(Status code, String message) {
    super(String.format("Error: %s", message));
    this.code = code;
  }

  public Status code() { return code; }

  public static final class IOError extends LoxError {
    public IOError(IOException cause) {
      super(Status.IO_ERROR, cause);
    }
  }

  public static final class SyntaxError extends LoxError {
    public SyntaxError(int line, String message) {
      super(Status.SYNTAX_ERROR, line, message);
    }
  }

  public static final class RuntimeError extends LoxError {
    public RuntimeError(int line, String message) {
      super(Status.RUNTIME_ERROR, line, message);
    }
    public RuntimeError(String message) {
      super(Status.RUNTIME_ERROR, message);
    }
  }
}
