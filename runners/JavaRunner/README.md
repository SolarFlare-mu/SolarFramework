# JavaRunner

`org.solarframework.mu:java-runner` — a Java application jar on [`RunnerAPI`](../RunnerAPI/README.md): `java [options] -jar app.jar [args]`, run from the jar's folder **on the host's own JVM** unless told otherwise.

```java
JavaRunner app = new JavaRunner(Path.of("apps/server.jar"), "--port=9000").option("-Xmx512m");
app.start();
```

`option(...)` adds a JVM option, `java(executable)` picks another JVM.
