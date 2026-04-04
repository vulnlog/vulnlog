# Vulnlog

## Use the Docker image

```shell
docker run --rm ghcr.io/vulnlog/vulnlog --help
```

```shell
docker run --rm -v $(pwd):/work ghcr.io/vulnlog/vulnlog validate /work/full-example.vl.yaml
```
