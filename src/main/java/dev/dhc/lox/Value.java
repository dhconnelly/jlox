package dev.dhc.lox;

import dev.dhc.lox.AstNode.Stmt;
import dev.dhc.lox.Error.RuntimeError;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

public sealed interface Value {
  enum Type {
    NIL,
    BOOL,
    NUM,
    STR,
    CALLABLE,
    CLASS,
    INSTANCE,
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

  enum FunctionType {
    FUNCTION,
    INITIALIZER,
  }

  record LoxFunction(String name, Environment closure, List<String> params, List<Stmt> body, FunctionType ftype)
      implements LoxCallable {
    @Override public String toString() { return String.format("<fn %s>", name); }
    @Override public int arity() { return params.size(); }
    @Override public Type type() { return Type.CALLABLE; }
    @Override public Value call(Evaluator eval, List<Value> arguments) {
      var returnValue = eval.call(this, closure, arguments);
      return (ftype == FunctionType.INITIALIZER) ? closure.getAt(0, "this") : returnValue;
    }
    public LoxFunction bind(LoxInstance instance) {
      var env = new Environment(closure);
      env.define("this", instance);
      return new LoxFunction(name, env, params, body, ftype);
    }
  }

  record LoxClass(String name, Optional<LoxClass> superclass, Map<String, LoxFunction> methods) implements Value, LoxCallable {
    @Override public String toString() { return name; }
    @Override public Type type() {return Type.CLASS;}
    @Override public int arity() {return findMethod("init").map(LoxFunction::arity).orElse(0);}
    @Override public Value call(Evaluator eval, List<Value> args) {
      var instance = new LoxInstance(this);
      findMethod("init").ifPresent(init -> init.bind(instance).call(eval, args));
      return instance;
    }
    public Optional<LoxFunction> findMethod(String name) {
      return Optional.ofNullable(methods.get(name))
          .or(() -> superclass.flatMap(sc -> sc.findMethod(name)));
    }
  }

  final class LoxInstance implements Value {
    private final LoxClass klass;
    private final Map<String, Value> fields = new HashMap<>();

    LoxInstance(LoxClass klass) {
      this.klass = klass;
    }

    @Override public String toString() { return String.format("%s instance", klass.name); }
    @Override public Type type() {return Type.INSTANCE;}

    public Value get(Token name) {
      if (fields.containsKey(name.cargo())) {
        return fields.get(name.cargo());
      }

      var method = klass.findMethod(name.cargo()).map(m -> m.bind(this));
      return method.orElseThrow(() ->
          new RuntimeError(name.line(), String.format("Undefined property '%s'.", name.cargo())));
    }

    public void set(Token name, Value value) {
      fields.put(name.cargo(), value);
    }
  }
}
