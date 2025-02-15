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

  private static final int SUCCESS = 0;
  private static final int FAILURE = 1;
  private static final int USAGE_ERROR = 64;

  public static int run(Environment env, String[] args) throws IOException {
    if (args.length == 0) {
      env.err.println("usage: lox COMMAND [ARG...]");
      return USAGE_ERROR;
    }

    final var command = args[0];
    switch (command) {
      case "tokenize" -> {
        if (args.length != 2) {
          env.err.println("usage: lox tokenize FILE");
          return USAGE_ERROR;
        }
        final var in = Files.newInputStream(Paths.get(args[1]));
        final var scanner = new Scanner(in);
        int code = SUCCESS;
        while (true) {
          try {
            final var token = scanner.nextToken();
            env.out.println(token);
            if (token.type() == Type.EOF) break;
          } catch (LoxError e) {
            env.err.println(e.getMessage());
            code = e.code();
          }
        }
        return code;
      }

      case "parse" -> {
        if (args.length != 2) {
          env.err.println("usage: lox parse FILE");
          return USAGE_ERROR;
        }
        final var in = Files.newInputStream(Paths.get(args[1]));
        final var scanner = new Scanner(in);
        final var parser = new Parser(scanner);
        try {
          while (!parser.eof()) {
            final var expr = parser.expr();
            env.out.println(expr);
          }
          return SUCCESS;
        } catch (LoxError e) {
          env.err.println(e.getMessage());
          return e.code();
        }
      }

      case "evaluate" -> {
        if (args.length != 2) {
          env.err.println("usage: lox evaluate FILE");
          return USAGE_ERROR;
        }
        final var in = Files.newInputStream(Paths.get(args[1]));
        final var scanner = new Scanner(in);
        final var parser = new Parser(scanner);
        final var evaluator = new Evaluator();
          while (!parser.eof()) {
            try {
            final var expr = parser.expr();
            final var value = evaluator.evaluate(expr);
            env.out.println(value);
        } catch (LoxError e) {
          env.err.println(e.getMessage());
          return e.code();
        }
          }
        return SUCCESS;
      }

      case "repl" -> {
        final var reader = new BufferedReader(new InputStreamReader(env.in));
        boolean hadError = false;
        while (true) {
          env.out.print("> ");
          final var line = reader.readLine();
          if (line == null) break;
          final var scanner = new Scanner(new ByteArrayInputStream(line.getBytes(UTF_8)));
          final var parser = new Parser(scanner);
          try {
            while (!parser.eof()) {
              final var expr = parser.expr();
              env.out.println(expr);
            }
          } catch (LoxError e) {
            env.err.println(e.getMessage());
            hadError = true;
          }
        }
        return hadError ? FAILURE : SUCCESS;
      }

      case "run" -> {
        if (args.length != 2) {
          env.err.println("usage: lox run FILE");
          return USAGE_ERROR;
        }
        final var in = Files.newInputStream(Paths.get(args[1]));
        final var scanner = new Scanner(in);
        final var parser = new Parser(scanner);
        try {
          while (!parser.eof()) {
            final var expr = parser.expr();
            env.out.println(expr);
          }
        } catch (LoxError e) {
          env.err.println(e.getMessage());
          return e.code();
        }
        return SUCCESS;
      }

      default -> {
        env.err.println("unrecognized command: " + command);
        return USAGE_ERROR;
      }
    }
  }

  public static void main(String[] args) throws IOException {
    final var env = new Environment(System.in, System.out, System.err);
    System.exit(run(env, args));
  }
}
