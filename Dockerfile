FROM alpine:3.12

# bash is needed to run stage script
RUN apk add --no-cache openjdk11 bash
COPY docs /docs
RUN adduser --uid 2004 --disabled-password --gecos "" docker
COPY target/universal/stage/ /workdir/
RUN chmod +x /workdir/bin/codacy-metrics-detekt
USER docker
WORKDIR /src
ENTRYPOINT ["/workdir/bin/codacy-metrics-detekt"]