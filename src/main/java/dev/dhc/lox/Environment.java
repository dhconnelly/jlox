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

  public Optional<Value> assign(String name, Value value) {
    if (values.containsKey(name)) {
      values.put(name, value);
      return Optional.of(value);
    }
    if (up.isPresent()) {
      return up.get().assign(name, value);
    }
    return Optional.empty();
  }

  public Optional<Value> get(String name) {
    final var value = values.get(name);
    if (value != null) return Optional.of(value);
    if (up.isPresent()) return up.get().get(name);
    return Optional.empty();
  }
}
