# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Bonus variant: a fully static, FROM scratch image.
#
# Everything is linked statically against musl (via Alpine) so the resulting
# binary has no runtime dependencies at all:
#   * Drogon + Trantor  -> static archives (BUILD_SHARED_LIBS=OFF)
#   * jsoncpp, uuid, zlib, openssl, c-ares -> "-static" archives
#   * libstdc++ / libgcc / libc -> linked in by "-static"
#
# Because there is no shell or libc in the final image, it also has no
# HEALTHCHECK - a scratch image cannot run curl/wget.
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
      file \
      ca-certificates \
      jsoncpp-dev \
      jsoncpp-static \
      util-linux-dev \
      util-linux-static \
      zlib-dev \
      zlib-static \
      openssl-dev \
      openssl-libs-static \
      c-ares-dev

# Build Drogon with static-only dependencies.
RUN git clone --depth 1 --branch ${DROGON_VERSION} --recurse-submodules \
      https://github.com/drogonframework/drogon.git /tmp/drogon \
 && cmake -S /tmp/drogon -B /tmp/drogon/build -G Ninja \
      -DCMAKE_BUILD_TYPE=Release \
      -DCMAKE_INSTALL_PREFIX=/usr/local \
      -DBUILD_SHARED_LIBS=OFF \
      -DUSE_STATIC_LIBS_ONLY=ON \
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
RUN cmake -S . -B build -G Ninja \
      -DCMAKE_BUILD_TYPE=Release \
      -DSTATIC_BUILD=ON \
 && cmake --build build --parallel "$(nproc)" \
  && strip --strip-all build/pastebin \
  && file build/pastebin \
  && file build/pastebin | grep -q static \
  && mkdir -p /out/app \
  && chown 65532:65532 /out/app

# ---------------------------------------------------------------------------
# Stage 2 - scratch
#   A single statically linked binary and nothing else.
# ---------------------------------------------------------------------------
FROM scratch AS runtime

# Drogon creates ./uploads/tmp/00 .. FF at startup relative to the working
# directory; it must be writable by the non-root user.
COPY --from=build --chown=65532:65532 /out/app /app
COPY --from=build /src/build/pastebin /pastebin
# CA bundle is not needed by this service, but copying it keeps outbound TLS usable.
COPY --from=build /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/ca-certificates.crt

# Numeric UID/GID: scratch has no /etc/passwd.
USER 65532:65532
WORKDIR /app
EXPOSE 8080
ENTRYPOINT ["/pastebin"]
