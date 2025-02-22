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
    } catch (Error e) {
      report(e);
      return e.code();
    } catch (IOException e) {
      report(e);
      return Status.IO_ERROR;
    }
  }

  private Status runInternal(Command cmd) throws IOException {
    return switch (cmd) {
      case Command.Tokenize(var path) -> {
        final var scanner = scanFile(path);
        var code = Status.SUCCESS;
        while (true) {
          try {
            final var tok = scanner.nextToken();
            out.println(tok);
            if (tok.type() == Type.EOF) break;
          } catch (Error e) {
            report(e);
            code = e.code();
          }
        }
        yield code;
      }

      case Command.Parse(var path) -> {
        try (var reader = Files.newBufferedReader(Paths.get(path))) {
          reader.lines().map(this::parse).map(Parser::expr).forEach(out::println);
        }
        yield Status.SUCCESS;
      }

      case Command.Evaluate(var path) -> {
        final var evaluator = new Evaluator(out);
        final var resolver = new Resolver();
        try (var reader = Files.newBufferedReader(Paths.get(path))) {
          final var exprs = reader.lines().map(this::parse).map(Parser::expr);
          exprs.forEach(expr -> {
            final var value = evaluator.evaluate(resolver.resolve(expr));
            out.println(value);
          });
        }
        yield Status.SUCCESS;
      }

      case Command.Run(var path) -> {
        final var program = parseFile(path).program();
        final var evaluator = new Evaluator(out);
        final var resolver = new Resolver();
        for (var stmt : program.stmts()) {
          evaluator.execute(resolver.resolve(stmt));
        }
        yield Status.SUCCESS;
      }

      case Command.Repl() -> {
        final var reader = new BufferedReader(new InputStreamReader(in));
        final var evaluator = new Evaluator(out);
        final var resolver = new Resolver();
        while (true) {
          out.print("> ");
          try {
            final var line = reader.readLine();
            if (line == null) break;
            final var stmt = parse(line).stmt();
            out.println(stmt);
            evaluator.execute(resolver.resolve(stmt));
          } catch (Exception e) {
            report(e);
          }
        }
        yield Status.SUCCESS;
      }
    };
  }

  private void report(Exception e) {
    if (e instanceof Error) {
      err.println(e.getMessage());
    } else {
      err.println(e);
    }
  }

  private Scanner scanFile(String path) throws IOException {
    return new Scanner(Files.newInputStream(Paths.get(path)));
  }

  private Parser parseFile(String path) throws IOException {
    return new Parser(scanFile(path));
  }

  private Parser parse(String text) {
    final var stream = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    return new Parser(new Scanner(stream));
  }
}
