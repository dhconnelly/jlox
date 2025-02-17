package dev.dhc.lox;

import dev.dhc.lox.AstNode.Stmt;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public sealed interface Value {
  enum Type {
    NIL,
    BOOL,
    NUM,
    STR,
    CALLABLE,
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
  sealed interface LoxCallable extends Value {
    int arity();
    Value call(Evaluator eval, List<Value> arguments);
  }
  record LoxNativeFunction(int arity, BiFunction<Evaluator, List<Value>, Value> f) implements LoxCallable {
    @Override public String toString() { return "<native fn>"; }
    @Override public Type type() { return Type.CALLABLE; }
    @Override public Value call(Evaluator eval, List<Value> arguments) {
      return f.apply(eval, arguments);
    }
  }
  record LoxFunction(String name, Environment closure, List<String> params, List<Stmt> body)
      implements LoxCallable {
    @Override public String toString() { return String.format("<fn \"%s\">", name); }
    @Override public int arity() { return params.size(); }
    @Override public Type type() { return Type.CALLABLE; }
    @Override public Value call(Evaluator eval, List<Value> arguments) {
      return eval.call(this, closure, arguments);
    }
  }
}
