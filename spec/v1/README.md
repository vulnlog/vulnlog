```shell
jsonschema metaschema schema/vulnlog.json --resolve schema/defs/
```

```shell
jsonschema lint schema/vulnlog.json --resolve schema/defs/
````

```shell
jsonschema fmt schema/vulnlog.json --resolve schema/defs/
```

```shell
jsonschema bundle schema/vulnlog.json -w --resolve schema/defs/ > generated/vulnlog-bundled.json
```
