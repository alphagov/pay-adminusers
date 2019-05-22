# govukpay/openjdk:adoptopenjdk-jre-11.0.3_7-alpine
FROM govukpay/openjdk@sha256:6cedbd1225f4fa204fb0e379f26eb9c859e2a1c1b47a6fe4c8f3309dfc3faae1

RUN apk --no-cache upgrade

RUN apk add --no-cache bash

ENV PORT 8080
ENV ADMIN_PORT 8081

EXPOSE 8080
EXPOSE 8081

WORKDIR /app

ADD docker-startup.sh /app/docker-startup.sh
ADD target/*.yaml /app/
ADD target/pay-*-allinone.jar /app/

CMD bash ./docker-startup.sh
