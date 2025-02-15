package dev.dhc.lox;

import dev.dhc.lox.Driver.Command;

public class Main {
  private static Command parseCommand(String[] args) {
    if (args.length == 0) return new Command.Repl();
    if (args.length == 1) return new Command.Run(args[0]);
    if (args.length > 2) {
      System.err.println("usage: lox [COMMAND [FILE]]");
      System.exit(Status.USAGE_ERROR.code());
    }
    final var path = args[1];
    return switch (args[0]) {
      case "tokenize" -> new Command.Tokenize(path);
      case "parse" -> new Command.Parse(path);
      case "evaluate" -> new Command.Evaluate(path);
      case "interpret" -> new Command.Run(path);
      default -> {
        System.err.println("invalid command");
        System.exit(Status.USAGE_ERROR.code());
        throw new AssertionError();
      }
    };
  }

  public static void main(String[] args) {
    final var command = parseCommand(args);
    final var driver = new Driver(System.in, System.out, System.err);
    final var result = driver.run(command);
    System.exit(result.code());
  }
}
