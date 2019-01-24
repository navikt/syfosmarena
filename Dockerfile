FROM navikt/java:11
COPY build/libs/syfosmarena-*-all.jar app.jar
COPY config/preprod/application.json application-preprod.json
COPY config/prod/application.json application-prod.json
ENV JAVA_OPTS="-Dlogback.configurationFile=logback-remote.xml"
ENV APPLICATION_PROFILE="remote"