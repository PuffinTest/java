package dev.puffintest.examples;

import dev.puffintest.PuffinTest;
import java.util.List;

import static dev.puffintest.PuffinTest.expectEqual;
import static dev.puffintest.PuffinTest.expectTrue;
import static dev.puffintest.PuffinTest.expectMatches;
import static dev.puffintest.PuffinTest.expectNotMatches;
import static dev.puffintest.PuffinTest.expectGreaterThan;
import static dev.puffintest.PuffinTest.expectLessThan;
import static dev.puffintest.PuffinTest.expectAtLeast;
import static dev.puffintest.PuffinTest.expectAtMost;
import static dev.puffintest.PuffinTest.expectNotBetween;
import static dev.puffintest.PuffinTest.test;

public final class BasicExample {
    private BasicExample() {}

    public static void main(String[] args) {
        int status = PuffinTest.runTests(List.of(
                test("formats a title", () -> expectEqual("Hello, Puffin", "Hello, Puffin")),
                test("finds the item", () -> {
                    expectTrue(List.of("puffin", "tern").contains("puffin"));
                    expectMatches("order-4821 accepted", "order-\\d+ accepted");
                    expectNotMatches("request failed", "\\b2xx\\b");
                    expectGreaterThan(5, 3);
                    expectLessThan(2, 3);
                    expectAtLeast(3, 3);
                    expectAtMost(3, 3);
                    expectNotBetween(4, 1, 3);
                    expectNotClose(0.1, 0.9, 1e-9, 0.0);
                })
        ));
        System.exit(status);
    }
}
