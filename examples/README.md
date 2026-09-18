# Dockerfile examples

Four independent sample projects that implement the **same tiny HTTP API** (a URL shortener / mini-pastebin), each packaged with Docker:

| Project | Language / framework | Build style | Base image variants | Host port | Swagger UI |
|---|---|---|---|---|---|
| [`python-fastapi/`](python-fastapi) | Python 3.12 / FastAPI | **single stage** | `debian`, `alpine` | 8000 / 8001 | `/docs` |
| [`java-spring/`](java-spring) | Java 25 / Spring Boot 3.5 | **multi-stage** | `debian`, `alpine` | 8100 / 8101 | `/swagger-ui.html` |
| [`csharp-aspnet/`](csharp-aspnet) | C# / .NET 8 (ASP.NET Core) | **multi-stage** | `debian`, `alpine` | 8200 / 8201 | `/swagger` |
| [`cpp-drogon/`](cpp-drogon) | C++17 / Drogon (CMake + Ninja) | **multi-stage** | `debian`, `alpine`, **`scratch`** | 8300 / 8301 / 8302 | — |

The Java, C# and C++ images are multi-stage builds: **the final image contains only the built artifact** (plus its runtime libraries).

The Python image is a single stage build: **the final image contains the source code of the project**.

Every implementation stores data **in memory only** - there is no database and no external service.

Restarting a container clears all pastes.

The API is identical across all four languages and is documented in [`docs/API.md`](docs/API.md).

---

## Published images

All images are built and pushed to the `lighthouse.tohka.us` registry:

```
lighthouse.tohka.us/example-projects/pastebin-python:debian
lighthouse.tohka.us/example-projects/pastebin-python:alpine
lighthouse.tohka.us/example-projects/pastebin-java:debian
lighthouse.tohka.us/example-projects/pastebin-java:alpine
lighthouse.tohka.us/example-projects/pastebin-csharp:debian
lighthouse.tohka.us/example-projects/pastebin-csharp:alpine
lighthouse.tohka.us/example-projects/pastebin-cpp:debian
lighthouse.tohka.us/example-projects/pastebin-cpp:alpine
lighthouse.tohka.us/example-projects/pastebin-cpp:scratch
```

Browse the layers at <https://lighthouse.tohka.us/example-projects>.

Pull, for example:

```bash
docker pull lighthouse.tohka.us/example-projects/pastebin-python:alpine
```

---

## Quick start

Run any variant with Docker Compose (builds the image first):

```bash
docker compose -f python-fastapi/debian/docker-compose.yml up --build -d
```

Then talk to it:

```bash
curl -s -X POST http://localhost:8000/paste \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://www.docker.com/"}'
# {"code":"jZTnNZmo","short_url":"/jZTnNZmo","long_url":"https://www.docker.com/"}

curl -i http://localhost:8000/jZTnNZmo
# HTTP/1.1 302 Found
# location: https://www.docker.com/
```

See [`docs/API.md`](docs/API.md) for the full API contract.
