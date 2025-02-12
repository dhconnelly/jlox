package dev.dhc.lox;

import java.io.PrintStream;

public class Interpreter {
  private final PrintStream out;
  private final PrintStream err;

  public Interpreter(PrintStream out, PrintStream err) {
    this.out = out;
    this.err = err;
  }

  public void execute(String text) {
    //
  }
}
