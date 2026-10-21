# How to run the catalog-service instrumentation

Plain Java 21, no dependencies: Maven is not installed on this machine.

## Install

Java 21 only. Check it:

```bash
java -version
```

## Build and test

From `03-instrumentation/catalog-service`:

```bash
mkdir out
javac -d out $(find src -name '*.java')
java -cp out co.edu.corhuila.csp.catalog.observability.TracePropagationTest
```

## Console output (my machine, Java 21.0.6)

```
tests: 5, passed: 5, failed: 0
```

The test exits with code 1 when a check fails. It proves that the `trace_id` received in the
`traceparent` header is the one stored in the `outbox_event` document, that a malformed header starts a new
trace, and that `RequestMetrics.render()` prints the counter by outcome and the latency histogram.
