# Digest-pinned so Dependabot can see the base image; the tag alone never moves.
FROM alpine:latest@sha256:f59abe656b692d901e184be3445e084786602108599d2a51d229c0f5a48c71bf

COPY build/native/nativeCompile/vulnlog /usr/local/bin/vulnlog

RUN addgroup -g 1000 vulnlog && adduser -u 1000 -G vulnlog -D vulnlog && apk add --no-cache gcompat
USER vulnlog
WORKDIR /work

ENTRYPOINT ["vulnlog"]
