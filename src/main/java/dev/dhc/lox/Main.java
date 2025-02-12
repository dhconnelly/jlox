package dev.dhc.lox;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
  private final BufferedReader in;
  private final PrintStream out;
  private final PrintStream err;

  public Main(InputStream in, OutputStream out, OutputStream err) {
    this.in = new BufferedReader(new InputStreamReader(in));
    this.out = new PrintStream(out);
    this.err = new PrintStream(err);
  }

  public int run(String[] args) throws IOException {
    if (args.length == 0) {
      err.println("usage: lox COMMAND [ARG...]");
      return 1;
    }

    final var command = args[0];
    switch (command) {
      case "tokenize" -> {
        out.println("EOF  null");
        return 0;
      }

      case "repl" -> {
        final var lox = new Interpreter(out, err);
        while (true) {
          out.print("> ");
          final var line = in.readLine();
          if (line == null) break;
          lox.execute(line);
        }
        return 0;
      }

      case "run" -> {
        if (args.length < 2) {
          err.println("usage: lox run FILE");
          return 1;
        }
        final var text = Files.readString(Paths.get(args[1]), UTF_8);
        final var lox = new Interpreter(out, err);
        lox.execute(text);
        return 0;
      }

      default -> {
        err.println("unrecognized command: " + command);
        return 64;
      }
    }
  }

  public static void main(String[] args) throws IOException {
    final var env = new Main(System.in, System.out, System.err);
    final int result = env.run(args);
    System.exit(result);
  }
}
