FROM ghcr.io/navikt/baseimages/temurin:17
COPY syfosmarena-stream/build/libs/*-all.jar app.jar
ENV JAVA_OPTS='-Dlogback.configurationFile=logback.xml'
