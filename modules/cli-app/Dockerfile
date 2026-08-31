# Digest-pinned so Dependabot can see the base image; the tag alone never moves.
FROM debian:trixie-slim@sha256:d7e12182ce18b85b93007c1dedf31f2d29e01ccf3182cc4017c709b6259bc132

COPY build/native/nativeCompile/vulnlog /usr/local/bin/vulnlog

RUN groupadd -g 1000 vulnlog && useradd -u 1000 -g vulnlog vulnlog
USER vulnlog
WORKDIR /work

ENTRYPOINT ["vulnlog"]
