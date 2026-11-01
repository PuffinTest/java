package dev.puffintest.examples;

import dev.puffintest.PuffinTest;
import java.util.List;

import static dev.puffintest.PuffinTest.expectEqual;
import static dev.puffintest.PuffinTest.expectTrue;
import static dev.puffintest.PuffinTest.test;

public final class BasicExample {
    private BasicExample() {}

    public static void main(String[] args) {
        int status = PuffinTest.runTests(List.of(
                test("formats a title", () -> expectEqual("Hello, Puffin", "Hello, Puffin")),
                test("finds the item", () -> expectTrue(List.of("puffin", "tern").contains("puffin")))
        ));
        System.exit(status);
    }
}
