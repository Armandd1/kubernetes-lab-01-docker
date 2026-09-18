# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Stage 1 - build
#   Compiles Drogon (with Trantor) from source on Alpine/musl, then builds
#   the pastebin server with CMake + Ninja.
# ---------------------------------------------------------------------------
FROM alpine:3.24 AS build

ARG DROGON_VERSION=v1.9.13

RUN apk add --no-cache \
      build-base \
      cmake \
      ninja \
      git \
      pkgconf \
      linux-headers \
      ca-certificates \
      jsoncpp-dev \
      util-linux-dev \
      zlib-dev \
      openssl-dev \
      c-ares-dev

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
#   Only the compiled binary plus musl/libstdc++ and the shared dependencies.
# ---------------------------------------------------------------------------
FROM alpine:3.24 AS runtime

RUN apk add --no-cache \
      libstdc++ \
      ca-certificates \
      jsoncpp \
      libuuid \
      zlib \
      libssl3 \
      libcrypto3 \
      c-ares \
  && addgroup -S -g 1000 app \
  && adduser -S -G app -u 1000 app \
  && mkdir -p /app \
  && chown app:app /app

# Drogon creates ./uploads/tmp/00 .. FF at startup relative to the working
# directory; it must be writable by the non-root user.
WORKDIR /app

COPY --from=build /src/build/pastebin /usr/local/bin/pastebin

USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/health || exit 1
ENTRYPOINT ["/usr/local/bin/pastebin"]
