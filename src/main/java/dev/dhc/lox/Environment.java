package dev.dhc.lox;

import dev.dhc.lox.LoxError.RuntimeError;
import java.util.HashMap;
import java.util.Map;

public class Environment {
  private final Map<String, Value> globals = new HashMap<>();

  public Value defineGlobal(String name, Value value) {
    globals.put(name, value);
    return value;
  }

  public Value get(String name) {
    final var value = globals.get(name);
    if (value == null) throw new RuntimeError(String.format("Undefined variable: '%s'", name));
    return value;
  }
}
