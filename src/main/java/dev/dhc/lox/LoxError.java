package dev.dhc.lox;

public class LoxError extends Exception {
  public LoxError(int line, String message) {
    super(String.format("[line %d] Error: %s", line, message));
  }
}
