package dev.dhc.lox;

public sealed interface Value {
  record NilValue() implements Value {
    @Override public String toString() { return "nil"; }
  }
  record BoolValue(boolean value) implements Value {
    @Override public String toString() { return Boolean.toString(value); }
  }
  record NumValue(double value) implements Value {
    @Override public String toString() {
      final var s = Double.toString(value);
      return (s.endsWith(".0")) ? s.substring(0, s.length()-2) : s;
    }
  }
  record StrValue(String value) implements Value {
    @Override public String toString() { return value; }
  }
}
