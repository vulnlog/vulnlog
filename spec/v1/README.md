```shell
docker run --interactive --volume "$PWD:/workspace" ghcr.io/sourcemeta/jsonschema:v14.14.2 metaschema schema/vulnlog.json --resolve schema/defs/
```

Lint all schema files:

```shell
docker run --interactive --volume "$PWD:/workspace" ghcr.io/sourcemeta/jsonschema:v14.14.2 lint schema/
````

Format all schema files:

```shell
docker run --interactive --volume "$PWD:/workspace" ghcr.io/sourcemeta/jsonschema:v14.14.2 fmt schema/
```

Bundle all schema files:

```shell
docker run --interactive --volume "$PWD:/workspace" ghcr.io/sourcemeta/jsonschema:v14.14.2 bundle -w schema/vulnlog.json --resolve schema/defs/ > generated/vulnlog-bundled.json
```
