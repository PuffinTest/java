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

    /** Fail unless actual and unexpected differ, including nested arrays. */
    public static void expectNotEqual(Object actual, Object unexpected) {
        if (Objects.deepEquals(actual, unexpected)) {
            throw new ExpectationFailure("Values are unexpectedly equal", "a value different from " + display(unexpected), actual, true);
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

    /** Fail unless the given iterable, map, string, or array contains the item. */
    public static void expectContains(Object container, Object item) {
        boolean contains;
        if (container instanceof Iterable<?> values) {
            contains = false;
            for (Object value : values) if (Objects.deepEquals(value, item)) { contains = true; break; }
        } else if (container instanceof java.util.Map<?, ?> values) {
            contains = values.containsKey(item);
        } else if (container instanceof CharSequence text) {
            contains = item instanceof CharSequence needle && text.toString().contains(needle);
        } else if (container != null && container.getClass().isArray()) {
            contains = false;
            for (int index = 0; index < Array.getLength(container); index++) {
                if (Objects.deepEquals(Array.get(container, index), item)) { contains = true; break; }
            }
        } else {
            throw new IllegalArgumentException("container must be an iterable, map, string, or array");
        }
        if (!contains) {
            throw new ExpectationFailure("Item is not present in the container", "a container containing " + display(item), container, true);
        }
    }

    /** Require a callback to throw an exception of the requested type. */
    public static <T extends Throwable> T expectThrows(Class<T> expected, CheckedRunnable body) throws Exception {
        Objects.requireNonNull(expected, "expected");
        try {
            body.run();
        } catch (Throwable error) {
            if (error instanceof VirtualMachineError fatal) throw fatal;
            if (error instanceof ThreadDeath fatal) throw fatal;
            if (error instanceof LinkageError fatal) throw fatal;
            if (expected.isInstance(error)) return expected.cast(error);
            throw new ExpectationFailure("Expected " + expected.getSimpleName() + ", got " + error.getClass().getSimpleName(), expected.getSimpleName(), error.getClass().getSimpleName(), true);
        }
        throw new ExpectationFailure("Expected " + expected.getSimpleName() + " to be thrown", expected.getSimpleName(), "no exception", true);
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
