package dev.dhc.lox;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
  private final PrintStream out;
  private final PrintStream err;

  public Main(final PrintStream out, final PrintStream err) {
    this.out = out;
    this.err = err;
  }

  public int run(final String command, final InputStream source) {
    switch (command) {
      case "tokenize" -> {
        out.println("EOF  null");
        return 0;
      }
      default -> {
        err.println("unrecognized command: " + command);
        return 1;
      }
    }
  }

  public static void main(String[] args) {
    if (args.length < 2) {
      System.err.println("Usage: lox <command> <filename>");
      System.exit(1);
    }
    final var main = new Main(System.out, System.err);
    final var command = args[0];
    try (var source = Files.newInputStream(Paths.get(args[1]))) {
      System.exit(main.run(command, source));
    } catch (IOException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }
}
