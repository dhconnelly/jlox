package dev.dhc.lox;

import dev.dhc.lox.LoxError.IOError;
import dev.dhc.lox.Token.Type;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Driver {
  private final InputStream in;
  private final PrintStream out;
  private final PrintStream err;

  public Driver(InputStream in, PrintStream out, PrintStream err) {
    this.in = in;
    this.out = out;
    this.err = err;
  }

  private Scanner scan(Path path) {
    try {
      return new Scanner(Files.newInputStream(path));
    } catch (IOException e) {
      throw new IOError(e);
    }
  }

  private Parser parse(Path path) {
    return new Parser(scan(path));
  }

  private Parser parse(String text) {
    return new Parser(new Scanner(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))));
  }

  public sealed interface Command {
    record Tokenize(String path) implements Command {}
    record Parse(String path) implements Command {}
    record Evaluate(String path) implements Command {}
    record Run(String path) implements Command {}
    record Repl() implements Command {}
  }

  public Status run(Command cmd) {
    final var evaluator = new Evaluator(out);
    return switch (cmd) {
      case Command.Tokenize(var path) -> {
        final var scanner = scan(Paths.get(path));
        var code = Status.SUCCESS;
        while (true) {
          try {
            final var tok = scanner.nextToken();
            out.println(tok);
            if (tok.type() == Type.EOF) break;
          } catch (LoxError e) {
            err.println(e.getMessage());
            code = e.code();
          }
        }
        yield code;
      }

      case Command.Parse(var path) -> {
        final var parser = parse(Paths.get(path));
        try {
          while (!parser.eof()) {
            out.println(parser.expr());
          }
        } catch (LoxError e) {
          err.println(e.getMessage());
          yield e.code();
        }
        yield Status.SUCCESS;
      }

      case Command.Evaluate(var path) -> {
        final var parser = parse(Paths.get(path));
        var code = Status.SUCCESS;
        while (!parser.eof()) {
          try {
            out.println(evaluator.evaluate(parser.expr()));
          } catch (LoxError e) {
            err.println(e.getMessage());
            code = e.code();
          }
        }
        yield code;
      }

      case Command.Run(var path) -> {
        try {
          final var program = parse(Paths.get(path)).program();
          for (var stmt : program.stmts()) {
            evaluator.execute(stmt);
          }
        } catch (LoxError e) {
          err.println(e.getMessage());
          yield e.code();
        }
        yield Status.SUCCESS;
      }

      case Command.Repl() -> {
        final var reader = new BufferedReader(new InputStreamReader(in));
        var code = Status.SUCCESS;
        while (true) {
          out.print("> ");
          try {
            final var line = reader.readLine();
            if (line == null) break;
            evaluator.execute(parse(line).stmtOrDecl());
          } catch (LoxError e) {
            err.println(e.getMessage());
            code = e.code();
          } catch (IOException e) {
            err.println(e.getMessage());
            code = Status.IO_ERROR;
          }
        }
        yield code;
      }
    };
  }
}
