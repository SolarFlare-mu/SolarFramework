package org.solarframework.proxyserver.obj.domains;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

public abstract class BaseDomain<T> {

    /** Backend address to which requests are forwarded. */
    private final String ip;
    /** Host name matched by the proxy. */
    private final String name;
    /** Path used when routing requests to the backend. */
    private final String path;

    public String getIp() {
        return ip;
    }
    public String getName() {
        return name;
    }
    public String getPath() {
        return path;
    }

    public boolean isProxy() {
        return path != null && (path.startsWith("http://") || path.startsWith("https://"));
    }

    public abstract String getHost();

    /**
     * The file of this domain's folder a request path asks for: {@code index.html} for a slash, empty for anything that is no readable file of the folder - a path
     * that climbs out, a folder, or any segment that starts with a dot ({@code .env}, {@code .git/config}) except RFC 8615's {@code .well-known}, which a site is
     * meant to publish. The one check every implementation serves files through.
     */
    public Optional<Path> fileFor(String requestPath) {
        if (isProxy() || requestPath == null) return Optional.empty();
        String asked = requestPath.isEmpty() || requestPath.endsWith("/") ? requestPath + "index.html" : requestPath;
        if (Arrays.stream(asked.split("/")).anyMatch(s -> s.startsWith(".") && !s.equals(".well-known"))) return Optional.empty();
        Path base = Path.of(path).toAbsolutePath().normalize(), file = base.resolve(asked.replaceFirst("^/+", "")).normalize();
        return file.startsWith(base) && Files.isRegularFile(file) && Files.isReadable(file) ? Optional.of(file) : Optional.empty();
    }

    /** No no-arg constructor: Gson instantiates a JSON-loaded domain through {@code sun.misc.Unsafe} without calling one, same as every other JSONItem-backed class in this codebase. */
    protected BaseDomain(String ip, String name, String path) {
        this.name = name.replace("*.", "");
        this.ip = ip;
        this.path = path;
    }
}
