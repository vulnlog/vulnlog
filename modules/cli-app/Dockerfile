# Digest-pinned so Dependabot can see the base image; the tag alone never moves.
FROM debian:trixie-slim@sha256:3a39a0592364683e6bab97937b72cad5a8fa6dcbbee90edb3bb48c7f8e94f258

COPY build/native/nativeCompile/vulnlog /usr/local/bin/vulnlog

RUN groupadd -g 1000 vulnlog && useradd -u 1000 -g vulnlog vulnlog
USER vulnlog
WORKDIR /work

ENTRYPOINT ["vulnlog"]
