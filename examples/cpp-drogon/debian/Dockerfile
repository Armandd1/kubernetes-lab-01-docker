# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Stage 1 - build
#   Compiles Drogon (with Trantor) from source, then builds the pastebin
#   server with CMake + Ninja. The compiler, sources and build tree stay in
#   this stage and never reach the final image.
# ---------------------------------------------------------------------------
FROM debian:bookworm-slim AS build

ARG DROGON_VERSION=v1.9.13

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      build-essential \
      cmake \
      ninja-build \
      git \
      pkg-config \
      ca-certificates \
      libjsoncpp-dev \
      uuid-dev \
      zlib1g-dev \
      libssl-dev \
      libc-ares-dev \
 && rm -rf /var/lib/apt/lists/*

# Install Drogon into /usr/local (static libdrogon.a + libtrantor.a).
RUN git clone --depth 1 --branch ${DROGON_VERSION} --recurse-submodules \
      https://github.com/drogonframework/drogon.git /tmp/drogon \
 && cmake -S /tmp/drogon -B /tmp/drogon/build -G Ninja \
      -DCMAKE_BUILD_TYPE=Release \
      -DCMAKE_INSTALL_PREFIX=/usr/local \
      -DBUILD_CTL=OFF \
      -DBUILD_EXAMPLES=OFF \
      -DBUILD_TESTING=OFF \
      -DBUILD_ORM=OFF \
      -DBUILD_BROTLI=OFF \
      -DBUILD_YAML_CONFIG=OFF \
 && cmake --build /tmp/drogon/build --parallel "$(nproc)" \
 && cmake --install /tmp/drogon/build \
 && rm -rf /tmp/drogon

WORKDIR /src
COPY CMakeLists.txt ./
COPY src ./src
RUN cmake -S . -B build -G Ninja -DCMAKE_BUILD_TYPE=Release \
 && cmake --build build --parallel "$(nproc)"

# ---------------------------------------------------------------------------
# Stage 2 - runtime
#   Only the compiled binary plus the shared libraries it links against.
#   No compilers, no sources, no build tree.
# ---------------------------------------------------------------------------
FROM debian:bookworm-slim AS runtime

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      libjsoncpp25 \
      libssl3 \
      zlib1g \
      libc-ares2 \
      libuuid1 \
      libstdc++6 \
      ca-certificates \
      curl \
 && rm -rf /var/lib/apt/lists/* \
  && groupadd --gid 1000 app \
  && useradd --uid 1000 --gid 1000 --create-home --shell /usr/sbin/nologin app \
  && mkdir -p /app \
  && chown app:app /app

# Drogon creates ./uploads/tmp/00 .. FF at startup relative to the working
# directory; it must be writable by the non-root user.
WORKDIR /app

COPY --from=build /src/build/pastebin /usr/local/bin/pastebin

USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8080/health || exit 1
ENTRYPOINT ["/usr/local/bin/pastebin"]
