# Microservices and Kubernetes: Lab 1 - Docker

In Lab 1 you pick a programming language and framework of your choice, then build a small sample project around it. The project should expose some kind of service, for example an HTTP API.

The goal isn't to write a large application. It's to get comfortable packaging and running a service in a container, and to understand the moving parts around it: images, containers, ports, and volumes.

I have pushed this image at the following URL: https://hub.docker.com/r/armandd2/pastebin-ts (`docker.io/armandd2/pastebin-ts:latest`).

---

## What to do

1. Fill out this [Google Form](https://docs.google.com/forms/d/e/1FAIpQLSd1aPZAgC9bUHD3Kna0LtilYYpEPRpB3gH7BAJyVL_uvPYJjQ/viewform) with your real name and GitHub username.
2. Choose a programming language and framework of your choice.
3. Create a sample project with a `Dockerfile` that exposes a service (for example an HTTP API) on a port.
4. Fork this repository.
5. Push the service and its source code to your fork.
6. Build and push the Docker image to a registry of your choice (for example: [Docker Hub](https://hub.docker.com)), then update this README to point to it.

**AI usage is allowed and encouraged for this lab.** Use whatever assistant or tooling helps you move faster. You are still responsible for understanding what ends up in your `Dockerfile`.

## Checklist

Work through this before you submit.

- [x] I am using an up to date base image
- [x] If possible, I am caching all my dependencies before I build my final application.
- [x] Changing only my source code, but not dependencies, does not trigger a full dependency re-download and rebuild.
- [x] If possible, I am using a multistage build to hide my source code from the final image.
- [x] I have a **user** set up for my container, that is being used to run it.
- [x] I have all the ports I need to communicate to my container port forwarded using `ports`.
- [x] If I'm using any persistent data, I have it set up as a **volume**.
- [x] Extra points: a HEALTHCHECK is configured for my container.

---

## Submission

Fork this repository, push your service and its source code to your fork, and share or return the URL of your fork. That's it.

---

## Reference examples

The [`examples/`](examples) folder contains four independent sample projects that implement the same tiny HTTP API, a URL shortener / mini-pastebin. Each one is packaged with Docker.

- Folder: [`examples/`](examples)
- Overview and published images: [`examples/README.md`](examples/README.md)

---

## Further reading

- Docker documentation: <https://docs.docker.com/>
- The sample projects in this repository: [`examples/README.md`](examples/README.md)

---

## Docker basics

A hands-on cheat sheet for the commands you'll use most. Examples use `alpine` and `nginx:alpine`, which are small and pull quickly.

### Images: pulling, tagging, and deleting

An **image** is a read-only template. You pull it, tag it, inspect it, and eventually delete it.

Pull a specific tag versus the default `latest`:

```bash
docker pull alpine:3.24
docker pull alpine          # implicitly alpine:latest
```

Pinning a version (`alpine:3.24`) is safer than relying on `latest`: `latest` moves whenever the publisher pushes a new build, so the same command can produce a different image tomorrow.

List the images on your machine:

```bash
docker images          # also: docker image ls
docker images alpine   # filter by repository
```

Example output:

```text
REPOSITORY   TAG       IMAGE ID       CREATED        SIZE
alpine       3.20      9b8e5a1c2f3d   2 weeks ago    7.8MB
alpine       latest    9b8e5a1c2f3d   2 weeks ago    7.8MB
```

Tagging adds another name that points at the same image. It is not a copy:

```bash
docker tag alpine:3.24 myuser/alpine:mytag
```

Now `alpine:3.24` and `myuser/alpine:mytag` share one image ID. A **tag** is a human-readable label (`3.20`), while a **digest** is the immutable content hash (`sha256:...`). Two tags can point at the same digest, and a tag can be moved to a new digest later.

Inspect the full metadata, including the digest and the config:

```bash
docker image inspect alpine:3.24
docker image inspect -f '{{.Id}}' alpine:3.24
```

Delete images with `image rm`:

```bash
docker image rm alpine:3.24             # remove by tag
docker image rm 9b8e5a1c2f3d            # remove by image ID
docker image rm -f myuser/alpine:mytag  # force, also untags even if used
```

Tags that no longer point at any image are called **dangling**. Clean them up, or clean up everything unused:

```bash
docker image prune      # remove dangling images only
docker image prune -a   # remove ALL images not used by a container
```

---

### Base images

Prefer **Debian/Ubuntu or Alpine** base images, for example `debian`, `ubuntu`, or `alpine`. They are well documented, small enough, and behave like a normal Linux userland.

- `alpine` gives you the smallest images, but it uses `musl` instead of `glibc`. That's usually fine, though some precompiled binaries expect `glibc`.
- `debian`/`ubuntu` (and `-slim`) are a good default when you want compatibility with common packages.
- **`scratch`** is a bonus/advanced option. It's an empty starting point and only works if your binary is fully static (no shared libraries, no shell). The C++ example in the examples folder has a `scratch` variant if you want to see how that looks.

---

### Running images from Docker Hub

A **container** is a running (or stopped) instance of an image. When you run an image, the image itself never changes.

Running Alpine with no command exits immediately:

```bash
docker run alpine
```

Alpine's default command is a shell, but with no terminal attached it has nothing to read and exits. Compare with an interactive session:

```bash
docker run -it alpine sh
/ # echo hello
hello
/ # exit
```

The `-it` flag combines `-i` (keep STDIN open, interactive) and `-t` (allocate a pseudo-TTY). Use it whenever you want a shell inside the container.

**Foreground vs detached.** Without `-d`, the command runs in the foreground and blocks your terminal. With `-d`, Docker starts it in the background and prints the container ID:

```bash
docker run -d --name web -p 8080:80 nginx:alpine
```

List containers. `docker ps` shows only running ones; `docker ps -a` includes stopped ones:

```bash
docker ps
docker ps -a
```

Run a command inside a running container:

```bash
docker exec -it web sh
docker exec web cat /etc/os-release
```

Read the logs of a detached container:

```bash
docker logs web            # everything so far
docker logs -f web         # follow live output (Ctrl+C to stop watching)
docker logs --tail 50 web  # only the last 50 lines
```

Inspect container state and networking with `-f` (Go template) for a single value:

```bash
docker inspect web
docker inspect -f '{{.State.Status}}' web
docker inspect -f '{{.NetworkSettings.IPAddress}}' web
```

### Container lifecycle: image to container to running to stopped to removed

An **image** is a read-only template. A **container** is a running or stopped instance created from that image. The progression looks like this:

```text
image  ──docker create──►  container (created)  ──docker start──►  running
                                                                     │
                                                                docker stop
                                                                     ▼
                                                                 stopped
                                                                     │
                                                                 docker rm
                                                                     ▼
                                                                 removed
```

Create a container without starting it, then start, stop, and restart it:

```bash
docker create --name myapp nginx:alpine   # image -> created container
docker start myapp                        # created -> running
docker stop myapp                         # running -> stopped
docker restart myapp                      # stop + start in one step
```

`docker stop` sends `SIGTERM`, waits (10 seconds by default), then sends `SIGKILL`. `docker kill` skips the grace period and sends `SIGKILL` right away.

Remove a stopped container:

```bash
docker rm myapp      # only works on a stopped container
docker rm -f web     # force: stop it if needed, then remove
```

Remove all containers in one go. Be careful, this is destructive:

```bash
docker rm -f $(docker ps -aq)   # WARNING: force-removes every container, running or not
```

To remove only the stopped ones, including their anonymous volumes:

```bash
docker container prune
```

### Ports and EXPOSE

`EXPOSE 8080` in a Dockerfile is **documentation and metadata only**. It does not publish the port and does not make the service reachable from outside the container. It's a hint for humans.

You publish ports at runtime with `-p` / `--publish`, in `host:container` order:

```bash
docker run -d -p 8080:80 nginx:alpine          # host 8080 -> container 80
docker run -d -p 127.0.0.1:8080:80 nginx:alpine # bind to localhost only
docker run -d -p 80 nginx:alpine               # random host port -> container 80
```

Show the mappings for a running container:

```bash
docker port web
```

Example output:

```text
80/tcp -> 0.0.0.0:8080
```

A complete example you can curl:

```bash
docker run -d -p 8080:80 --name web nginx:alpine
curl localhost:8080
docker stop web && docker rm web
```

`-P` / `--publish-all` publishes every port declared with `EXPOSE` to a random host port. Either way, the rule is simple: a container is reachable from the host only if you publish (or use `-P`), because containers otherwise sit on their own internal network.

### Volumes

Containers are ephemeral by default. When you remove one, anything written to its filesystem is gone. Volumes give data a life outside the container.

**Bind mount (a folder on the host).** You choose the path, and Docker mounts it into the container:

```bash
docker run -d --name web -p 8080:80 \
  -v "$PWD/html:/usr/share/nginx/html:ro" \
  nginx:alpine
```

This is tight coupling to the host: the path must exist and it means something on this machine only, which is why it's great for local development.

The more explicit modern syntax uses `--mount`:

```bash
docker run -d --name web -p 8080:80 \
  --mount type=bind,src="$PWD/html",dst=/usr/share/nginx/html,readonly \
  nginx:alpine
```

**Named volume (managed by Docker).** You give it a name and Docker stores it under `/var/lib/docker/volumes`, so you never worry about host paths:

```bash
docker volume create mydata
docker run -d --name db -v mydata:/var/lib/nginx nginx:alpine

docker volume ls
docker volume inspect mydata
docker volume rm mydata
docker volume prune
```

The difference in one line: a **bind mount** points at a path you pick on the host; a **named volume** is storage Docker creates and manages.

Volumes outlive containers. Removing a container leaves named volumes in place:

```bash
docker rm db            # the named volume mydata still exists
docker rm -v db         # -v also removes anonymous volumes only
docker volume rm mydata # named volumes persist until you remove them explicitly
```

### Registries: logging in, tagging, and pushing

A **registry** is the server that stores and serves images. When an image name has no registry host in it, Docker assumes Docker Hub (`docker.io`). Images on Docker Hub use the shorthand `username/repository`; every other registry is addressed by hostname, for example `ghcr.io/username/repository` or `lighthouse.tohka.us/example-projects/pastebin-python`.

**Log in.** `docker login` authenticates against a registry and saves the credentials in `~/.docker/config.json`:

```bash
docker login                      # Docker Hub (default), prompts for username + password
docker login ghcr.io              # GitHub Container Registry
docker login lighthouse.tohka.us  # any other registry, addressed by hostname
```

Use a **personal access token** as the password, not your account password. For Docker Hub generate one under *Account settings -> Security*; for GitHub use a PAT with the `write:packages` scope.

`docker login` stores the credential base64-encoded (not encrypted) in `~/.docker/config.json`. On a shared machine, configure a credential helper or log out when you're done:

```bash
docker logout             # Docker Hub
docker logout ghcr.io     # a specific registry
```

**Tag an image for a registry.** The full name of an image is `[registry host/][namespace/]repository[:tag]`. Only the repository is required: the registry defaults to Docker Hub and the tag defaults to `latest`. Tagging just adds a name that points at the same image, so do it before pushing:

```bash
# Docker Hub: <username>/<repository>:<tag>
docker tag pastebin:local myuser/pastebin:v1

# GitHub Container Registry: ghcr.io/<owner>/<image>:<tag>
docker tag pastebin:local ghcr.io/myuser/pastebin:v1
```

You can also name the image correctly at build time and skip the separate `docker tag` step:

```bash
docker build -t myuser/pastebin:v1 .
```

**Push.** `docker push` uploads every layer the registry doesn't already have:

```bash
docker push myuser/pastebin:v1
docker push ghcr.io/myuser/pastebin:v1
```

The name you push must match the tag exactly. If you get `denied: requested access to the resource is denied`, you are either not logged in or pushing under a namespace you don't own.

Pushing `latest` is a convention, but pin real versions too so consumers can request a stable image:

```bash
docker tag pastebin:local myuser/pastebin:v1
docker tag pastebin:local myuser/pastebin:latest
docker push myuser/pastebin:v1
docker push myuser/pastebin:latest
```
