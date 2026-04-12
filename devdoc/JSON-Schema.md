Check all schema files:

```shell
docker run --interactive --volume "$PWD:/workspace" ghcr.io/sourcemeta/jsonschema:v14.20.2 metaschema schema/vulnlog-v1.json --resolve schema/v1/defs/
```

Lint all schema files:

```shell
docker run --interactive --volume "$PWD:/workspace" ghcr.io/sourcemeta/jsonschema:v14.20.2 lint schema/
````

Format all schema files:

```shell
docker run --interactive --volume "$PWD:/workspace" ghcr.io/sourcemeta/jsonschema:v14.20.2 fmt schema/
```

Bundle all schema files:

```shell
docker run --interactive --volume "$PWD:/workspace" ghcr.io/sourcemeta/jsonschema:v14.20.2 bundle \
-w schema/v1/vulnlog.json \
--resolve schema/v1/defs/ > schema/vulnlog-v1.json
```
