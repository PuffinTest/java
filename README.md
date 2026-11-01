# PuffinTest for Java

PuffinTest is an early Java library for named test cases and readable expectation failures. This first slice provides equality, truth and falsehood checks, explicit failure, independent sequential execution, and a `0` or `1` process status from the runner.

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
    test("greets the user", () -> expectEqual("HELLO", "hello".toUpperCase()))
));
System.exit(status);
```

Each case runs independently. Failures show the case name, message, expected and actual values when available, and the first stack location outside PuffinTest. The runner returns `1` when a case fails or when no cases are provided.

This initial Java implementation has not yet been verified. It does not yet include discovery, filtering, skips, tags, fixtures, or a dedicated test suite.
