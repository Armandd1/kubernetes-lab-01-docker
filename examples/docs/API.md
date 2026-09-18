# Shared API — Mini Pastebin

All four implementations (Python/FastAPI, Java/Spring Boot, C#/ASP.NET Core,
C++/Drogon) expose the **same** HTTP API. The service is a tiny pastebin for
URLs: you `POST` a long URL, you get back an 8-character code, and `GET /{code}`
redirects to the original URL.

There is **no database** — every implementation stores the URL↔code mapping
**in memory only**. Restarting the container (or recreating it) clears all
pastes. This is intentional: the projects are meant to demonstrate Dockerfiles
and `docker-compose`, not persistence.

Container port is always **8080**. Host ports differ per variant so several
variants can run at the same time (see the root `README.md`).

---

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/paste` | Create a short code for a long URL |
| `GET`  | `/{code}` | Redirect to the long URL for an 8-char code |
| `GET`  | `/health` | Liveness probe (used by `HEALTHCHECK`) |
| `GET`  | `/` | Service metadata (name + docs path) |

Swagger UI is available where the language/framework supports it:

| Implementation | Swagger UI | OpenAPI JSON |
|---|---|---|
| Python / FastAPI | `/docs` (and `/redoc`) | `/openapi.json` |
| Java / Spring Boot | `/swagger-ui.html` | `/v3/api-docs` |
| C# / ASP.NET Core | `/swagger` | `/swagger/v1/swagger.json` |
| C++ / Drogon | — (not supported) | — |

---

## `POST /paste`

Creates a short code that maps to the supplied long URL.

**Request**

```http
POST /paste HTTP/1.1
Host: localhost:8000
Content-Type: application/json

{ "url": "https://example.com/a/very/long/path?with=query" }
```

**Success — `201 Created`**

```json
{
  "code": "Ab12Cd34",
  "short_url": "/Ab12Cd34",
  "long_url": "https://example.com/a/very/long/path?with=query"
}
```

- `code` — 8 random characters from `A-Z`, `a-z`, `0-9` (62⁸ ≈ 2.2×10¹⁴ combinations).
- `short_url` — a **relative** path, so it works behind any host/proxy. Prepend your own base URL.
- Codes are unique within a running process; collisions are retried.

**Errors**

| Status | Body | When |
|--------|------|------|
| `400` | `{"error":"url is required"}` | `url` missing or blank |
| `400` | `{"error":"url must be a valid http(s) URL"}` | `url` does not begin with `http://` or `https://` |

```bash
curl -i -X POST http://localhost:8000/paste \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://example.com/a/very/long/path"}'
```

---

## `GET /{code}`

Redirects to the long URL stored for `code`.

- `code` must be exactly 8 characters from `[A-Za-z0-9]`; anything else is treated as not found.
- **`302 Found`** with a `Location` header pointing at the long URL (empty body).
- **`404 Not Found`** with `{"error":"not found"}` when the code does not exist.

```bash
# -L follows the redirect; -i shows the 302 + Location header
curl -i http://localhost:8000/Ab12Cd34
curl -L http://localhost:8000/Ab12Cd34
```

---

## `GET /health`

Always returns `200 OK` while the process is running. Used by each Dockerfile's
`HEALTHCHECK` and by the `docker-compose.yml` healthcheck.

```json
{ "status": "ok" }
```

---

## `GET /`

Returns basic service metadata, including the Swagger path when available.

```json
{ "service": "pastebin-python", "docs": "/docs" }
```

The `service` value is `pastebin-python`, `pastebin-java`, `pastebin-csharp`,
or `pastebin-cpp` depending on the implementation.

---

## End-to-end example

```bash
# 1. Start any variant (example: Python on Debian, host port 8000)
docker compose -f python-fastapi/debian/docker-compose.yml up --build -d

# 2. Create a short code
curl -s -X POST http://localhost:8000/paste \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://www.docker.com/"}'
# {"code":"Xy7Qp2Lm","short_url":"/Xy7Qp2Lm","long_url":"https://www.docker.com/"}

# 3. Follow the redirect
curl -i http://localhost:8000/Xy7Qp2Lm
# HTTP/1.1 302 Found
# location: https://www.docker.com/

# 4. Unknown code
curl -i http://localhost:8000/aaaaaaaa
# HTTP/1.1 404 Not Found
# {"error":"not found"}
```

---

## Error format

Every error response is JSON with a single `error` key:

```json
{ "error": "url is required" }
```
