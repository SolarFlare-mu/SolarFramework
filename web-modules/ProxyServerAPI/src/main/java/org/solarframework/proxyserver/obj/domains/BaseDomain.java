package org.solarframework.proxyserver.obj.domains;

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

    /** No no-arg constructor: Gson instantiates a JSON-loaded domain through {@code sun.misc.Unsafe} without calling one, same as every other JSONItem-backed class in this codebase. */
    protected BaseDomain(String ip, String name, String path) {
        this.name = name.replace("*.", "");
        this.ip = ip;
        this.path = path;
    }
}
