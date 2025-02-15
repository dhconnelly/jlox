package dev.dhc.lox;

import dev.dhc.lox.Error.RuntimeError;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Environment {
  private final Optional<Environment> up;
  private final Map<String, Value> values = new HashMap<>();

  public Environment() {
    up = Optional.empty();
  }

  public Environment(Environment up) {
    this.up = Optional.of(up);
  }

  public void define(String name, Value value) {
    values.put(name, value);
  }

  private static RuntimeError undefined(String name) {
    return new RuntimeError(String.format("Undefined variable: '%s'", name));
  }

  public Value assign(String name, Value value) {
    return Optional.ofNullable(values.get(name))
        .map(ignored -> values.put(name, value))
        .or(() -> up.map(parent -> parent.assign(name, value)))
        .orElseThrow(() -> undefined(name));
  }

  public Value get(String name) {
    return Optional.ofNullable(values.get(name))
        .or(() -> up.map(parent -> parent.get(name)))
        .orElseThrow(() -> undefined(name));
  }
}
