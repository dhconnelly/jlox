package dev.dhc.lox;

public enum Status {
  // TODO: what is IO_ERROR supposed to be?
  SUCCESS(0), FAILURE(1), IO_ERROR(1), USAGE_ERROR(64), SYNTAX_ERROR(65), RUNTIME_ERROR(70);
  private final int code;
  Status(int code) {
    this.code = code;
  }
  public int code() {
    return code;
  }
}
