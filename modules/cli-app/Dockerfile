# Digest-pinned so Dependabot can see the base image; the tag alone never moves.
FROM alpine:latest@sha256:294b683cb724975bec92580e1e685676bd4b50bda910ddb8c51d4cabeaec77e6

COPY build/native/nativeCompile/vulnlog /usr/local/bin/vulnlog

RUN addgroup -g 1000 vulnlog && adduser -u 1000 -G vulnlog -D vulnlog && apk add --no-cache gcompat
USER vulnlog
WORKDIR /work

ENTRYPOINT ["vulnlog"]
