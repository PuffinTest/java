# PuffinTest for Java

PuffinTest is an early Java library for named test cases and readable expectation failures. It provides equality and inequality checks, truth and falsehood checks, length and emptiness checks for collections, maps, strings, and arrays, positive and negative membership checks for iterables, maps, strings, and arrays, typed exception checks, explicit failure, tags, skips, name and tag filters, per-case hooks, fail-fast mode, independent sequential execution, and a `0` or `1` process status from the runner.

Use `expectClose(actual, expected, relTolerance, absTolerance)` for approximate numeric checks and `expectBetween(actual, minimum, maximum)` for an inclusive numeric range.

The package is under development and has not been published to Maven Central. It requires Java 17 or newer and uses Maven for compilation.

## Run the example

From this directory:

```sh
mvn compile
java -cp target/classes dev.puffintest.examples.BasicExample
```

Use static imports to define cases:

```java
import static dev.puffintest.PuffinTest.*;
import java.util.List;

int status = runTests(List.of(
    test("greets the user", () -> {
        expectEqual("HELLO", "hello".toUpperCase());
        expectLength(List.of("north", "south"), 2);
        expectLengthBetween(List.of("north", "south"), 1, 3);
        expectStartsWith("puffin test", "puffin");
        expectContains("puffin test", "test");
        expectNotContains("puffin test", "penguin");
        expectNotEndsWith("puffin test", "bird");
        expectNull(null);
        expectNotNull("puffin");
        expectEmpty(List.of());
        expectNotEmpty(List.of("puffin"));
        expectBetween(18, 13, 19);
    })
));
System.exit(status);
```

Each case runs independently. Failures show the case name, message, expected and actual values when available, and the first stack location outside PuffinTest. The runner returns `1` when a case fails or when no cases are provided.

`expectLengthBetween(actual, minimum, maximum)` checks an inclusive range for arrays, collections, maps, and strings. `expectEmpty` and `expectNotEmpty` use the same supported values. The bounds must be nonnegative and ordered.

Use `test(name, tags, skipReason, body)` for tags or skips. `runTests(cases, new PuffinTest.RunOptions(filter, tag, failFast, beforeEach, afterEach))` applies optional name and tag filters, per-case hooks, and fail-fast behavior. Pass `null` for unused text filters or hooks; use `PuffinTest.RunOptions.defaults()` for the default behavior.

This initial Java implementation has not yet been verified. It does not yet include discovery, filtering, skips, tags, fixtures, or a dedicated test suite.
