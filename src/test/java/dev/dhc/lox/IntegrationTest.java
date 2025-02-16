package dev.dhc.lox;

import static java.nio.charset.StandardCharsets.UTF_8;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import dev.dhc.lox.Driver.Command;
import dev.dhc.lox.Driver.Command.Evaluate;
import dev.dhc.lox.Driver.Command.Parse;
import dev.dhc.lox.Driver.Command.Run;
import dev.dhc.lox.Driver.Command.Tokenize;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith({SnapshotExtension.class})
public class IntegrationTest {
  private record Result(int code, List<String> outLines, List<String> errLines) {}

  private Result execute(Command command) {
    final var in = new ByteArrayInputStream(new byte[]{});
    final var out = new ByteArrayOutputStream();
    final var err = new ByteArrayOutputStream();
    final var exit = new Driver(in, new PrintStream(out), new PrintStream(err)).run(command).code();
    return new Result(
        exit,
        out.toString(UTF_8).lines().toList(),
        err.toString(UTF_8).lines().toList());
  }

  private String resourcePath(String resource) {
    final var url = getClass().getClassLoader().getResource(resource);
    if (url == null) throw new RuntimeException("resource not found: " + resource);
    return Paths.get(url.getPath()).toFile().getAbsolutePath();
  }

  private Expect expect;

  @ParameterizedTest
  @ValueSource(strings = {
      "inputs/tokenize/test.lox",
      "inputs/tokenize/empty.lox",
      "inputs/tokenize/tokens.lox",
      "inputs/tokenize/scanner_errors.lox"
  })
  void testTokenize(String resource) {
    expect.scenario(resource).toMatchSnapshot(execute(new Tokenize(resourcePath(resource))));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "inputs/parse/empty.lox",
      "inputs/parse/expressions.lox",
      "inputs/parse/values.lox",
      "inputs/parse/parser_errors.lox",
  })
  void testParse(String resource) {
    expect.scenario(resource).toMatchSnapshot(execute(new Parse(resourcePath(resource))));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "inputs/evaluate/empty.lox",
      "inputs/evaluate/values.lox",
      "inputs/evaluate/unary_error.lox",
      "inputs/evaluate/plus_bad_types_error.lox",
      "inputs/evaluate/plus_same_types_error.lox",
      "inputs/evaluate/control_flow.lox",
      "inputs/evaluate/call_errors.lox",
  })
  void testEvaluate(String resource) {
    expect.scenario(resource).toMatchSnapshot(execute(new Evaluate(resourcePath(resource))));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "inputs/interpret/statements.lox",
      "inputs/interpret/undefined_error.lox",
      "inputs/interpret/scope.lox",
      "inputs/interpret/control_flow.lox",
      "inputs/interpret/functions.lox",
  })
  void testInterpret(String resource) {
    expect.scenario(resource).toMatchSnapshot(execute(new Run(resourcePath(resource))));
  }
}
