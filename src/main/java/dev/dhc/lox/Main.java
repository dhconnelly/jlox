package dev.dhc.lox;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class Main {
  private final PrintStream out;
  private final PrintStream err;

  public enum ExitCode {
    OK(0),
    FAILURE(1);

    public final int code;

    ExitCode(int code) {
      this.code = code;
    }
  }

  public enum Command {
    TOKENIZE("tokenize");

    public final String text;

    Command(String text) {
      this.text = text;
    }
  }

  public Main(final PrintStream out, final PrintStream err) {
    this.out = out;
    this.err = err;
  }

  public ExitCode run(final Command command, final InputStream source) {
    System.out.println("EOF  null");
    return ExitCode.OK;
  }

  public static void main(String[] args) {
    if (args.length < 2) {
      System.err.println("Usage: lox <command> <filename>");
      System.exit(1);
    }

    Command command = null;
    try {
      command = Command.valueOf(args[0].toUpperCase());
    } catch (IllegalArgumentException e) {
      System.err.println("invalid command");
      System.err.println("available commands: " + Arrays.toString(Command.values()));
      System.exit(1);
    }

    final var main = new Main(System.out, System.err);
    try (var source = Files.newInputStream(Paths.get(args[1]))) {
      final var exit = main.run(command, source);
      System.exit(exit.code);
    } catch (IOException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }
}
