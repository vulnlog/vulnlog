# Docker

## Use the Docker image

```shell
docker run --rm ghcr.io/vulnlog/vulnlog --help
```

```shell
docker run --rm -v $(pwd):/work ghcr.io/vulnlog/vulnlog validate /work/acme-shop.vl.yaml
```

## Build locally

Make sure the native image is available under `build/native/nativeCompile/vulnlog`. If not, build it first as described
in [Build the native binary](../CONTRIBUTING.md#build-the-native-binary).

Build the Docker image locally:

```shell
docker build -t vulnlog ./modules/cli-app/
```

Run it locally:

```shell
docker run --rm vulnlog --help
```
