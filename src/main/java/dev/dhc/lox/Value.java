package dev.dhc.lox;

public sealed interface Value {
  enum Type {
    NIL,
    BOOL,
    NUM,
    STR,
  }

  Type type();

  record NilValue() implements Value {
    @Override public String toString() { return "nil"; }
    @Override public Type type() { return Type.NIL; }
  }
  record BoolValue(boolean value) implements Value {
    @Override public String toString() { return Boolean.toString(value); }
    @Override public Type type() { return Type.BOOL; }
  }
  record NumValue(double value) implements Value {
    @Override public String toString() {
      final var s = Double.toString(value);
      return (s.endsWith(".0")) ? s.substring(0, s.length()-2) : s;
    }
    @Override public Type type() { return Type.NUM; }
  }
  record StrValue(String value) implements Value {
    @Override public String toString() { return value; }
    @Override public Type type() { return Type.STR; }
  }
}
