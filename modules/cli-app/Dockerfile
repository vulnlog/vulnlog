FROM debian:trixie-slim@sha256:cedb1ef40439206b673ee8b33a46a03a0c9fa90bf3732f54704f99cb061d2c5a

COPY build/native/nativeCompile/vulnlog /usr/local/bin/vulnlog

RUN groupadd -g 1000 vulnlog && useradd -u 1000 -g vulnlog vulnlog
USER vulnlog
WORKDIR /work

ENTRYPOINT ["vulnlog"]
