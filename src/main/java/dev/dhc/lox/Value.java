package dev.dhc.lox;

public sealed interface Value {
  record NilValue() implements Value {}
  record BoolValue(boolean value) implements Value {}
  record NumValue(double value) implements Value {}
  record StrValue(String value) implements Value {}
}
