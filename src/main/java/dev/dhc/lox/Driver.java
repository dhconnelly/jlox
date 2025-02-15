package dev.dhc.lox;

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

  public sealed interface Command {
    record Tokenize(String path) implements Command {}
    record Parse(String path) implements Command {}
    record Evaluate(String path) implements Command {}
    record Run(String path) implements Command {}
    record Repl() implements Command {}
  }

  public Status run(Command cmd) {
    try {
      return runInternal(cmd);
    } catch (LoxError e) {
      err.println(e.getMessage());
      return e.code();
    } catch (IOException e) {
      err.println(e.getMessage());
      return Status.IO_ERROR;
    }
  }

  private Status runInternal(Command cmd) throws IOException {
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
        final var parser = new Parser(scan(Paths.get(path)));
        while (!parser.eof()) {
          out.println(parser.expr());
        }
        yield Status.SUCCESS;
      }

      case Command.Evaluate(var path) -> {
        final var parser = new Parser(scan(Paths.get(path)));
        final var evaluator = new Evaluator(out);
        while (!parser.eof()) {
          out.println(evaluator.evaluate(parser.expr()));
        }
        yield Status.SUCCESS;
      }

      case Command.Run(var path) -> {
        final var program = new Parser(scan(Paths.get(path))).program();
        final var evaluator = new Evaluator(out);
        for (var stmt : program.stmts()) {
          evaluator.execute(stmt);
        }
        yield Status.SUCCESS;
      }

      case Command.Repl() -> {
        final var reader = new BufferedReader(new InputStreamReader(in));
        final var evaluator = new Evaluator(out);
        while (true) {
          out.print("> ");
          try {
            final var line = reader.readLine();
            if (line == null) break;
            final var stream = new ByteArrayInputStream(line.getBytes(StandardCharsets.UTF_8));
            final var stmt = new Parser(new Scanner(stream)).stmtOrDecl();
            evaluator.execute(stmt);
          } catch (Exception e) {
            err.println(e.getMessage());
          }
        }
        yield Status.SUCCESS;
      }
    };
  }

  private Scanner scan(Path path) throws IOException {
    return new Scanner(Files.newInputStream(path));
  }
}
