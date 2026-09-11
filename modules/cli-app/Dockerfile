# Digest-pinned so Dependabot can see the base image; the tag alone never moves.
FROM alpine:latest@sha256:28bd5fe8b56d1bd048e5babf5b10710ebe0bae67db86916198a6eec434943f8b

COPY build/native/nativeCompile/vulnlog /usr/local/bin/vulnlog

RUN addgroup -g 1000 vulnlog && adduser -u 1000 -G vulnlog -D vulnlog && apk add --no-cache gcompat
USER vulnlog
WORKDIR /work

ENTRYPOINT ["vulnlog"]
