package dev.dhc.lox;

import static java.nio.charset.StandardCharsets.UTF_8;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import dev.dhc.lox.Main.Environment;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith({SnapshotExtension.class})
public class IntegrationTest {
  private record Result(int code, List<String> outLines, List<String> errLines) {}

  private Result execute(String... args) throws IOException {
    final var in = new ByteArrayInputStream(new byte[]{});
    final var out = new ByteArrayOutputStream();
    final var err = new ByteArrayOutputStream();
    final var exit = Main.run(
        new Environment(in, new PrintStream(out), new PrintStream(err)), args);
    return new Result(
        exit,
        out.toString(UTF_8).lines().toList(),
        err.toString(UTF_8).lines().toList());
  }

  private static List<String> testResources() {
    return List.of("test.lox", "empty.lox", "tokens.lox");
  }

  private String resourcePath(String resource) {
    final var url = getClass().getClassLoader().getResource(resource);
    if (url == null) throw new RuntimeException("resource not found: " + resource);
    return Paths.get(url.getPath()).toFile().getAbsolutePath();
  }

  private Expect expect;

  @ParameterizedTest
  @MethodSource("testResources")
  void testScanner(String resource) throws IOException {
      expect.scenario(resource).toMatchSnapshot(execute("tokenize", resourcePath(resource)));
  }
}
