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
    if (values.containsKey(name)) {
      values.put(name, value);
      return value;
    }
    if (up.isPresent()) {
      return up.get().assign(name, value);
    }
    throw undefined(name);
  }

  public Value get(String name) {
    final var value = values.get(name);
    if (value != null) return value;
    if (up.isPresent()) return up.get().get(name);
    throw undefined(name);
  }
}
