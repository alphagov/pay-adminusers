FROM govukpay/openjdk:8-jre-alpine

ARG CHAMBER_URL=https://github.com/segmentio/chamber/releases/download/v1.9.0/chamber-v1.9.0-linux-amd64

RUN apk update
RUN apk upgrade

RUN apk add --no-cache bash

ADD chamber.sha256sum /tmp/chamber.sha256sum
RUN apk add --no-cache openssl && \
    mkdir -p bin && \
    wget -qO bin/chamber $CHAMBER_URL && \
    sha256sum -c /tmp/chamber.sha256sum && \
    rm /tmp/chamber.sha256sum && \
    chmod 755 bin/chamber && \
    apk del --purge openssl

ENV PORT 8080
ENV ADMIN_PORT 8081

EXPOSE 8080
EXPOSE 8081

WORKDIR /app

ADD target/*.yaml /app/
ADD target/pay-*-allinone.jar /app/
ADD docker-startup.sh /app/docker-startup.sh
ADD docker-startup-with-db-migration.sh /app/docker-startup-with-db-migration.sh
ADD run-with-chamber.sh /app/run-with-chamber.sh

CMD bash ./docker-startup.sh
