# Dockerfile for Hypertrace Java Agent base image
# This Dockerfile sets up a base image with the Hypertrace Java agent and necessary configurations for tracing Java applications.

FROM eclipse-temurin:8-jdk

# Copy agent and config to hypertrace directory
RUN mkdir -p /hypertrace
COPY hypertrace-agent-1.3.24-all.jar /hypertrace/
COPY hypertrace-config.yaml /hypertrace/ 

# Hypertrace configuration file
ENV HT_CONFIG_FILE=/hypertrace/hypertrace-config.yaml

# Batch Span Processor configuration for faster span export
ENV OTEL_BSP_SCHEDULE_DELAY=50
ENV OTEL_BSP_MAX_EXPORT_BATCH_SIZE=10
ENV OTEL_BSP_MAX_QUEUE_SIZE=20

# OpenTelemetry Java agent configuration
ENV OTEL_JAVAAGENT_ENABLED=true
ENV OTEL_JAVAAGENT_DEBUG=true
ENV OTEL_INSTRUMENTATION_ENABLED=true
ENV OTEL_PROPAGATORS=tracecontext,baggage,b3

# Disable database instrumentation
ENV OTEL_INSTRUMENTATION_SPRING_DATA_ENABLED=false
ENV OTEL_INSTRUMENTATION_JDBC_ENABLED=false
ENV OTEL_INSTRUMENTATION_MONGO_ENABLED=false

# Enable spring web framework instrumentation
ENV OTEL_INSTRUMENTATION_COMMON_DEFAULT_ENABLED=true
ENV OTEL_INSTRUMENTATION_SPRING_WEB_ENABLED=true

# === Set the entrypoint to use the Hypertrace agent with OTel configuration ===
ENTRYPOINT ["java", "-javaagent:/hypertrace/hypertrace-agent-1.3.24-all.jar"]