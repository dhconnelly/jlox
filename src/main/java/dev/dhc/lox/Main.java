package dev.dhc.lox;

import static java.nio.charset.StandardCharsets.UTF_8;

import dev.dhc.lox.Token.Type;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
  public record Environment(InputStream in, PrintStream out, PrintStream err) {}

  public static int run(Environment env, String[] args) throws IOException {
    if (args.length == 0) {
      env.err.println("usage: lox COMMAND [ARG...]");
      return 64;
    }

    final var command = args[0];
    switch (command) {
      case "tokenize" -> {
        if (args.length != 2) {
          env.err.println("usage: lox tokenize FILE");
          return 64;
        }
        final var in = Files.newInputStream(Paths.get(args[1]));
        final var scanner = new Scanner(in);
        boolean hadError = false;
        while (true) {
          try {
            final var token = scanner.nextToken();
            env.out.println(token);
            if (token.type() == Type.EOF) break;
          } catch (LoxError e) {
            env.err.println(e.getMessage());
            hadError = true;
          }
        }
        return hadError ? 65 : 0;
      }

      case "repl" -> {
        final var reader = new BufferedReader(new InputStreamReader(env.in));
        boolean hadError = false;
        while (true) {
          env.out.print("> ");
          final var line = reader.readLine();
          if (line == null) break;
          final var scanner = new Scanner(new ByteArrayInputStream(line.getBytes(UTF_8)));
          try {
            while (true) {
              final var token = scanner.nextToken();
              env.out.println(token);
              if (token.type() == Type.EOF) break;
            }
          } catch (LoxError e) {
            env.err.println(e.getMessage());
            hadError = true;
          }
        }
        return hadError ? 65 : 0;
      }

      case "run" -> {
        if (args.length != 2) {
          env.err.println("usage: lox run FILE");
          return 64;
        }
        final var in = Files.newInputStream(Paths.get(args[1]));
        final var scanner = new Scanner(in);
        try {
          while (true) {
            final var token = scanner.nextToken();
            env.out.println(token);
            if (token.type() == Type.EOF) break;
          }
        } catch (LoxError e) {
          env.err.println(e.getMessage());
          return 65;
        }
        return 0;
      }

      default -> {
        env.err.println("unrecognized command: " + command);
        return 64;
      }
    }
  }

  public static void main(String[] args) throws IOException {
    final var env = new Environment(System.in, System.out, System.err);
    System.exit(run(env, args));
  }
}
