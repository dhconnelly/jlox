package dev.dhc.lox;

import static java.nio.charset.StandardCharsets.UTF_8;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import dev.dhc.lox.Main.Command;
import dev.dhc.lox.Main.ExitCode;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith({SnapshotExtension.class})
public class IntegrationTest {
  private record Result(ExitCode exit, String out, String err) {}

  private Result execute(Command command, String resourceName) {
    final var out = new ByteArrayOutputStream();
    final var err = new ByteArrayOutputStream();
    final var main = new Main(new PrintStream(out), new PrintStream(err));
    final var source = getClass().getClassLoader().getResourceAsStream(resourceName);
    final var exit = main.run(command, source);
    return new Result(exit, out.toString(UTF_8), err.toString(UTF_8));
  }

  private static List<String> testResources() {
    return List.of("test.lox", "empty.lox");
  }

  private Expect expect;

  @ParameterizedTest
  @MethodSource("testResources")
  void testScanner(String resourceName) {
      expect.scenario(resourceName).toMatchSnapshot(execute(Command.TOKENIZE, resourceName));
  }
}
