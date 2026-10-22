FROM eclipse-temurin:8-jdk

RUN mkdir -p /otel

COPY opentelemetry-javaagent.jar /otel
COPY otel.properties              /otel

#ENV OTEL_SERVICE_NAME=default-service

ENTRYPOINT ["java", "-javaagent:/otel/opentelemetry-javaagent.jar", "-Dotel.javaagent.configuration-file=/otel/otel.properties"]
