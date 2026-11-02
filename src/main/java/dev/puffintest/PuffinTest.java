package dev.puffintest;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Small named test cases and readable expectation failures. */
public final class PuffinTest {
    private PuffinTest() {}

    @FunctionalInterface
    public interface CheckedRunnable {
        void run() throws Exception;
    }

    public record TestCase(String name, CheckedRunnable body, Set<String> tags, String skipReason) {
        public TestCase {
            if (name == null || name.isBlank()) throw new IllegalArgumentException("test name must not be blank");
            Objects.requireNonNull(body, "test body");
            tags = Set.copyOf(Objects.requireNonNull(tags, "tags"));
            if (tags.stream().anyMatch(tag -> tag == null || tag.isBlank())) throw new IllegalArgumentException("tags must be nonempty");
            if (skipReason != null && skipReason.isBlank()) throw new IllegalArgumentException("skipReason must not be blank");
        }

        public TestCase(String name, CheckedRunnable body) { this(name, body, Set.of(), null); }
    }

    public record RunOptions(String filter, String tag, boolean failFast, CheckedRunnable beforeEach, CheckedRunnable afterEach) {
        public static RunOptions defaults() { return new RunOptions(null, null, false, null, null); }
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

    /** Declare a tagged or skipped case. */
    public static TestCase test(String name, Set<String> tags, String skipReason, CheckedRunnable body) {
        return new TestCase(name, body, tags, skipReason);
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

    /** Fail unless two finite numbers are close within relative or absolute tolerance. */
    public static void expectClose(double actual, double expected, double relTolerance, double absTolerance) {
        if (Double.isNaN(relTolerance) || Double.isNaN(absTolerance) || relTolerance < 0 || absTolerance < 0 ||
                Double.isInfinite(relTolerance) || Double.isInfinite(absTolerance)) {
            throw new IllegalArgumentException("tolerances must be finite and nonnegative");
        }
        double difference = Math.abs(actual - expected);
        double scale = Math.max(Math.abs(actual), Math.abs(expected));
        boolean close = actual == expected || (Double.isFinite(actual) && Double.isFinite(expected) &&
                (difference <= absTolerance || (scale != 0 && difference / scale <= relTolerance)));
        if (!close) throw new ExpectationFailure("Values are not close", expected, actual, true);
    }

    /** Fail if two numbers are close within relative or absolute tolerance. */
    public static void expectNotClose(double actual, double expected, double relTolerance, double absTolerance) {
        if (Double.isNaN(relTolerance) || Double.isNaN(absTolerance) || relTolerance < 0 || absTolerance < 0 ||
                Double.isInfinite(relTolerance) || Double.isInfinite(absTolerance)) {
            throw new IllegalArgumentException("tolerances must be finite and nonnegative");
        }
        double difference = Math.abs(actual - expected);
        double scale = Math.max(Math.abs(actual), Math.abs(expected));
        boolean close = actual == expected || (Double.isFinite(actual) && Double.isFinite(expected) &&
                (difference <= absTolerance || (scale != 0 && difference / scale <= relTolerance)));
        if (close) throw new ExpectationFailure("Values are unexpectedly close", "not close to " + expected, actual, true);
    }

    /** Fail unless actual is between minimum and maximum, inclusive. */
    public static void expectBetween(double actual, double minimum, double maximum) {
        if (Double.isNaN(minimum) || Double.isNaN(maximum) || minimum > maximum) {
            throw new IllegalArgumentException("minimum must be less than or equal to maximum");
        }
        if (!(actual >= minimum && actual <= maximum)) {
            throw new ExpectationFailure("Value is outside the expected range", "between " + minimum + " and " + maximum + ", inclusive", actual, true);
        }
    }

    /** Fail if actual is within the inclusive range. */
    public static void expectNotBetween(double actual, double minimum, double maximum) {
        if (Double.isNaN(minimum) || Double.isNaN(maximum) || minimum > maximum) {
            throw new IllegalArgumentException("minimum must be less than or equal to maximum");
        }
        if (!(actual < minimum || actual > maximum)) {
            throw new ExpectationFailure("Value is inside the unexpected range", "outside [" + minimum + ", " + maximum + "]", actual, true);
        }
    }

    /** Fail unless actual is strictly greater than threshold. */
    public static void expectGreaterThan(double actual, double threshold) {
        if (!(actual > threshold)) throw new ExpectationFailure("Value is not greater than the threshold", "greater than " + threshold, actual, true);
    }

    /** Fail unless actual is strictly less than threshold. */
    public static void expectLessThan(double actual, double threshold) {
        if (!(actual < threshold)) throw new ExpectationFailure("Value is not less than the threshold", "less than " + threshold, actual, true);
    }

    /** Fail unless actual is greater than or equal to threshold. */
    public static void expectAtLeast(double actual, double threshold) {
        if (!(actual >= threshold)) throw new ExpectationFailure("Value is below the minimum threshold", "at least " + threshold, actual, true);
    }

    /** Fail unless actual is less than or equal to threshold. */
    public static void expectAtMost(double actual, double threshold) {
        if (!(actual <= threshold)) throw new ExpectationFailure("Value is above the maximum threshold", "at most " + threshold, actual, true);
    }

    /** Fail unless actual is true. */
    public static void expectTrue(boolean actual) {
        if (!actual) throw new ExpectationFailure("Expected true", true, false, true);
    }

    /** Fail unless actual is false. */
    public static void expectFalse(boolean actual) {
        if (actual) throw new ExpectationFailure("Expected false", false, true, true);
    }

    /** Fail unless actual is null. */
    public static void expectNull(Object actual) {
        if (actual != null) throw new ExpectationFailure("Expected null", null, actual, true);
    }

    /** Fail unless actual is non-null. */
    public static void expectNotNull(Object actual) {
        if (actual == null) throw new ExpectationFailure("Expected a non-null value", "non-null", null, true);
    }

    /** Fail unless the given iterable, map, string, or array contains the item. */
    public static void expectContains(Object container, Object item) {
        if (!containsValue(container, item)) {
            throw new ExpectationFailure("Item is not present in the container", "a container containing " + display(item), container, true);
        }
    }

    /** Fail if the given iterable, map, string, or array contains the item. */
    public static void expectNotContains(Object container, Object item) {
        if (containsValue(container, item)) {
            throw new ExpectationFailure("Item is unexpectedly present in the container", "a container not containing " + display(item), container, true);
        }
    }

    /** Fail unless actual has exactly the expected number of items, characters, or entries. */
    public static void expectLength(Object actual, int expected) {
        if (expected < 0) throw new IllegalArgumentException("expected length must be nonnegative");
        int actualLength = lengthOf(actual);
        if (actualLength != expected) throw new ExpectationFailure("Value has an unexpected length", expected, actualLength, true);
    }

    /** Fail unless actual has a length in the inclusive range. */
    public static void expectLengthBetween(Object actual, int minimum, int maximum) {
        if (minimum < 0 || maximum < 0 || minimum > maximum) {
            throw new IllegalArgumentException("length bounds must be nonnegative and ordered");
        }
        int actualLength = lengthOf(actual);
        if (actualLength < minimum || actualLength > maximum) {
            throw new ExpectationFailure("Value length is outside the expected range", "length between " + minimum + " and " + maximum + ", inclusive", actualLength, true);
        }
    }

    /** Fail unless actual has no items, characters, or entries. */
    public static void expectEmpty(Object actual) {
        expectLength(actual, 0);
    }

    /** Fail unless actual contains at least one item, character, or entry. */
    public static void expectNotEmpty(Object actual) {
        int actualLength = lengthOf(actual);
        if (actualLength == 0) throw new ExpectationFailure("Expected a non-empty value", "at least 1 item", 0, true);
    }

    /** Fail unless actual starts with the expected prefix. */
    public static void expectStartsWith(CharSequence actual, CharSequence prefix) {
        Objects.requireNonNull(actual, "actual");
        Objects.requireNonNull(prefix, "prefix");
        if (!actual.toString().startsWith(prefix.toString())) {
            throw new ExpectationFailure("Text does not start with the expected prefix", "text starting with " + display(prefix), actual, true);
        }
    }

    /** Fail unless actual ends with the expected suffix. */
    public static void expectEndsWith(CharSequence actual, CharSequence suffix) {
        Objects.requireNonNull(actual, "actual");
        Objects.requireNonNull(suffix, "suffix");
        if (!actual.toString().endsWith(suffix.toString())) {
            throw new ExpectationFailure("Text does not end with the expected suffix", "text ending with " + display(suffix), actual, true);
        }
    }

    /** Fail if actual starts with the given prefix. */
    public static void expectNotStartsWith(CharSequence actual, CharSequence prefix) {
        Objects.requireNonNull(actual, "actual");
        Objects.requireNonNull(prefix, "prefix");
        if (actual.toString().startsWith(prefix.toString())) {
            throw new ExpectationFailure("Text unexpectedly starts with the prefix", "text not starting with " + display(prefix), actual, true);
        }
    }

    /** Fail if actual ends with the given suffix. */
    public static void expectNotEndsWith(CharSequence actual, CharSequence suffix) {
        Objects.requireNonNull(actual, "actual");
        Objects.requireNonNull(suffix, "suffix");
        if (actual.toString().endsWith(suffix.toString())) {
            throw new ExpectationFailure("Text unexpectedly ends with the suffix", "text not ending with " + display(suffix), actual, true);
        }
    }

    private static int lengthOf(Object value) {
        if (value instanceof CharSequence text) return text.length();
        if (value instanceof Collection<?> collection) return collection.size();
        if (value instanceof java.util.Map<?, ?> map) return map.size();
        if (value != null && value.getClass().isArray()) return Array.getLength(value);
        throw new IllegalArgumentException("value must be a collection, map, string, or array");
    }

    private static boolean containsValue(Object container, Object item) {
        if (container instanceof Iterable<?> values) {
            for (Object value : values) if (Objects.deepEquals(value, item)) return true;
            return false;
        }
        if (container instanceof java.util.Map<?, ?> values) return values.containsKey(item);
        if (container instanceof CharSequence text) return item instanceof CharSequence needle && text.toString().contains(needle);
        if (container != null && container.getClass().isArray()) {
            for (int index = 0; index < Array.getLength(container); index++) {
                if (Objects.deepEquals(Array.get(container, index), item)) return true;
            }
            return false;
        }
        throw new IllegalArgumentException("container must be an iterable, map, string, or array");
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

    /** Fail unless substring occurs in error.getMessage(). */
    public static void expectErrorMessage(Throwable error, String substring) {
        Objects.requireNonNull(error, "error");
        Objects.requireNonNull(substring, "substring");
        String actual = error.getMessage() == null ? "" : error.getMessage();
        if (!actual.contains(substring)) {
            throw new ExpectationFailure("Error message does not contain the expected text", "error message containing " + display(substring), actual, true);
        }
    }

    /** Fail unless the regular expression matches somewhere in actual. */
    public static void expectMatches(CharSequence actual, String regex) {
        Objects.requireNonNull(actual, "actual");
        Objects.requireNonNull(regex, "regex");
        if (!Pattern.compile(regex).matcher(actual).find()) {
            throw new ExpectationFailure("Text does not match the expected pattern", "text matching " + display(regex), actual, true);
        }
    }

    /** Fail if the regular expression matches anywhere in actual. */
    public static void expectNotMatches(CharSequence actual, String regex) {
        Objects.requireNonNull(actual, "actual");
        Objects.requireNonNull(regex, "regex");
        if (Pattern.compile(regex).matcher(actual).find()) {
            throw new ExpectationFailure("Text unexpectedly matches the pattern", "text not matching " + display(regex), actual, true);
        }
    }

    /** Explicitly fail the current case. */
    public static void fail(String message) {
        throw new ExpectationFailure(message, null, null, false);
    }

    /** Run cases in order, continue after each failure, and return 0 or 1. */
    public static int runTests(List<TestCase> cases) {
        return runTests(cases, RunOptions.defaults());
    }

    /** Run matching cases with optional hooks and fail-fast behavior. */
    public static int runTests(List<TestCase> cases, RunOptions options) {
        Objects.requireNonNull(cases, "cases");
        Objects.requireNonNull(options, "options");
        List<TestCase> selected = cases.stream().filter(testCase ->
                (options.filter() == null || testCase.name().toLowerCase(Locale.ROOT).contains(options.filter().toLowerCase(Locale.ROOT))) &&
                (options.tag() == null || testCase.tags().stream().anyMatch(value -> value.equalsIgnoreCase(options.tag())))
        ).toList();
        int passed = 0;
        int failed = 0;
        int skipped = 0;
        int processed = 0;
        for (TestCase testCase : selected) {
            processed++;
            if (testCase.skipReason() != null) {
                System.out.println("SKIP  " + testCase.name() + " (" + testCase.skipReason() + ")");
                skipped++;
                continue;
            }
            boolean setupSucceeded = false;
            String phase = "beforeEach";
            boolean caseFailed = false;
            try {
                if (options.beforeEach() != null) options.beforeEach().run();
                setupSucceeded = true;
                phase = "test";
                testCase.body().run();
                phase = "afterEach";
                if (options.afterEach() != null) options.afterEach().run();
                System.out.println("PASS  " + testCase.name());
                passed++;
            } catch (Exception | AssertionError error) {
                failed++;
                caseFailed = true;
                printFailure(testCase, error, phase);
            }
            if (setupSucceeded && !phase.equals("afterEach") && options.afterEach() != null && caseFailed) {
                try {
                    options.afterEach().run();
                } catch (Exception | AssertionError error) {
                    failed++;
                    printFailure(testCase, error, "afterEach");
                }
            }
            if (caseFailed && options.failFast()) break;
        }
        System.out.printf("%n%d processed: %d passed, %d failed, %d skipped%n", processed, passed, failed, skipped);
        if (selected.isEmpty()) {
            if (options.filter() != null && options.tag() != null) System.out.printf("No test cases matched name '%s' and tag '%s'%n", options.filter(), options.tag());
            else if (options.filter() != null) System.out.println("No test cases matched name '" + options.filter() + "'");
            else if (options.tag() != null) System.out.println("No test cases matched tag '" + options.tag() + "'");
            else System.out.println("No test cases were found");
            return 1;
        }
        return failed == 0 ? 0 : 1;
    }

    private static void printFailure(TestCase testCase, Throwable error, String phase) {
        System.out.println("FAIL  " + testCase.name() + (phase.equals("test") ? "" : " (" + phase + ")"));
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
