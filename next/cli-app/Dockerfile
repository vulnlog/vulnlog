FROM debian:trixie-slim

COPY build/native/nativeCompile/vulnlog /usr/local/bin/vulnlog

RUN groupadd -g 1000 vulnlog && useradd -u 1000 -g vulnlog vulnlog
USER vulnlog
WORKDIR /work

ENTRYPOINT ["vulnlog"]
