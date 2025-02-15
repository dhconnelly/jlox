package dev.dhc.lox;

import java.io.IOException;

public sealed abstract class Error extends RuntimeException {
  private final Status code;

  public Error(Status code, int line, String message) {
    super(String.format("[line %d] Error: %s", line, message));
    this.code = code;
  }

  public Error(Status code, Throwable cause) {
    super(cause);
    this.code = code;
  }

  public Error(Status code, String message) {
    super(String.format("Error: %s", message));
    this.code = code;
  }

  public Status code() { return code; }

  public static final class IOError extends Error {
    public IOError(IOException cause) {
      super(Status.IO_ERROR, cause);
    }
  }

  public static final class SyntaxError extends Error {
    public SyntaxError(int line, String message) {
      super(Status.SYNTAX_ERROR, line, message);
    }
  }

  public static final class RuntimeError extends Error {
    public RuntimeError(int line, String message) {
      super(Status.RUNTIME_ERROR, line, message);
    }
    public RuntimeError(String message) {
      super(Status.RUNTIME_ERROR, message);
    }
  }
}
