# Docker

## Use the Docker image

```shell
docker run --rm ghcr.io/vulnlog/vulnlog --help
```

```shell
docker run --rm -v $(pwd):/work ghcr.io/vulnlog/vulnlog validate /work/full-example.vl.yaml
```

## Build locally

Make sure the native image is available under `build/native/nativeCompile/vulnlog`. If not use the Graal 25 JDK to build it with `./gradlew nativeCompile --no-configuration-cache` first.

Build the Docker image locally:

```shell
docker build -t vulnlog ./next/cli-app/
```

Run it locally:

```shell
docker run --rm vulnlog --help
```
