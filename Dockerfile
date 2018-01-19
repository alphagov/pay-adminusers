FROM govukpay/openjdk:8-jre-alpine


RUN apk update
RUN apk upgrade

RUN apk add --no-cache bash

RUN apk add --no-cache openssl && \
    mkdir -p bin && \
    apk del --purge openssl

ENV PORT 8080
ENV ADMIN_PORT 8081

EXPOSE 8080
EXPOSE 8081

WORKDIR /app


ADD chamber--linux-amd64 /app/
ADD target/*.yaml /app/
ADD target/pay-*-allinone.jar /app/
ADD docker-startup.sh /app/docker-startup.sh
ADD docker-startup-with-db-migration.sh /app/docker-startup-with-db-migration.sh
ADD run-with-chamber.sh /app/run-with-chamber.sh

CMD bash ./docker-startup.sh
