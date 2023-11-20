FROM eclipse-temurin:11-jre-alpine@sha256:1017b1b26f9b92921a93a730fa33327d83cdfa0cc2f7fa486b1348ad4eda755c

RUN ["apk", "--no-cache", "upgrade"]

ARG DNS_TTL=15

# Default to UTF-8 file.encoding
ENV LANG C.UTF-8

RUN echo networkaddress.cache.ttl=$DNS_TTL >> "$JAVA_HOME/conf/security/java.security"

COPY ./import_aws_rds_cert_bundles.sh /
RUN /import_aws_rds_cert_bundles.sh && rm /import_aws_rds_cert_bundles.sh

RUN ["apk", "add", "--no-cache", "bash", "tini"]

ENV PORT 8080
ENV ADMIN_PORT 8081

EXPOSE 8080
EXPOSE 8081

WORKDIR /app

COPY docker-startup.sh /app/docker-startup.sh
COPY target/*.yaml /app/
COPY target/pay-*-allinone.jar /app/

ENTRYPOINT ["tini", "-e", "143", "--"]

CMD ["bash", "./docker-startup.sh"]
