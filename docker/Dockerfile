FROM openjdk:8-stretch

COPY entrypoint.sh /sbin/entrypoint.sh
RUN chmod 755 /sbin/entrypoint.sh

EXPOSE 8081/tcp

COPY deploy /image

WORKDIR /image/app

ENV ALLOC_KEYSTORE_LOCATION=/image/local/jetty-keystore \
    ALLOC_KEYSTORE_PASSWORD=password \
    ALLOC_PORT=8080 \
    ALLOC_SSL_PORT=8081 

ENTRYPOINT ["/sbin/entrypoint.sh"]

