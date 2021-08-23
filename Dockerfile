# Base java:8
FROM java:8

# Add Rest Gateway war to container
ADD target/xrd-kafka-adapter-*.jar xrd-kafka-adapter.jar

# Entry with exec
ENTRYPOINT exec java $JAVA_OPTS -jar /xrd-kafka-adapter.jar

# Expose default port
EXPOSE 8080