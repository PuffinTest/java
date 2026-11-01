package dev.puffintest;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Small named test cases and readable expectation failures. */
public final class PuffinTest {
    private PuffinTest() {}

    @FunctionalInterface
    public interface CheckedRunnable {
        void run() throws Exception;
    }

    public record TestCase(String name, CheckedRunnable body) {
        public TestCase {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("test name must not be blank");
            Objects.requireNonNull(body, "test body");
        }
    }

    private static final class ExpectationFailure extends AssertionError {
        private final Object expected;
        private final Object actual;
        private final boolean hasValues;

        private ExpectationFailure(String message, Object expected, Object actual, boolean hasValues) {
            super(message);
            this.expected = expected;
            this.actual = actual;
            this.hasValues = hasValues;
        }
    }

    /** Declare a named test case. */
    public static TestCase test(String name, CheckedRunnable body) {
        return new TestCase(name, body);
    }

    /** Fail unless actual and expected are equal, including nested arrays. */
    public static void expectEqual(Object actual, Object expected) {
        if (!Objects.deepEquals(actual, expected)) {
            throw new ExpectationFailure("Values are not equal", expected, actual, true);
        }
    }

    /** Fail unless actual is true. */
    public static void expectTrue(boolean actual) {
        if (!actual) throw new ExpectationFailure("Expected true", true, false, true);
    }

    /** Fail unless actual is false. */
    public static void expectFalse(boolean actual) {
        if (actual) throw new ExpectationFailure("Expected false", false, true, true);
    }

    /** Explicitly fail the current case. */
    public static void fail(String message) {
        throw new ExpectationFailure(message, null, null, false);
    }

    /** Run cases in order, continue after each failure, and return 0 or 1. */
    public static int runTests(List<TestCase> cases) {
        Objects.requireNonNull(cases, "cases");
        int passed = 0;
        int failed = 0;
        for (TestCase testCase : cases) {
            try {
                testCase.body().run();
                System.out.println("PASS  " + testCase.name());
                passed++;
            } catch (Exception | AssertionError error) {
                failed++;
                printFailure(testCase, error);
            }
        }
        System.out.printf("%n%d processed: %d passed, %d failed%n", cases.size(), passed, failed);
        if (cases.isEmpty()) System.out.println("No test cases were found");
        return failed == 0 && !cases.isEmpty() ? 0 : 1;
    }

    private static void printFailure(TestCase testCase, Throwable error) {
        System.out.println("FAIL  " + testCase.name());
        System.out.println("      " + error.getClass().getSimpleName() + ": " + error.getMessage());
        if (error instanceof ExpectationFailure failure && failure.hasValues) {
            System.out.println("      Expected: " + display(failure.expected));
            System.out.println("      Actual:   " + display(failure.actual));
        }
        Arrays.stream(error.getStackTrace())
                .filter(frame -> !frame.getClassName().equals(PuffinTest.class.getName()))
                .findFirst()
                .ifPresent(frame -> System.out.printf("      %s:%d%n", frame.getFileName(), frame.getLineNumber()));
    }

    private static String display(Object value) {
        if (value == null) return "null";
        if (!value.getClass().isArray()) return String.valueOf(value);
        int length = Array.getLength(value);
        List<String> items = new ArrayList<>(length);
        for (int index = 0; index < length; index++) items.add(display(Array.get(value, index)));
        return items.toString();
    }
}
