# CertificateGenerator

`org.solarframework.mu:certificate-generator` — locally trusted HTTPS certificates, used by the three proxy servers.

- **`MKCert`** — downloads the `mkcert` binary for this OS and CPU (Windows, macOS, Linux; it throws on anything else), installs its local CA, and `GenerateCertificateFor(domains)` issues one certificate for every name into `config/certs` under `SolarHome`.
- **`OpenSSL`** — the same `IsDownloaded` / `Download` / `Install` for OpenSSL.

Installing the CA changes the machine's trust store, so the operating system may ask the user to confirm it on the first run.
