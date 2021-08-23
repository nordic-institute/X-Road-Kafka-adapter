# X-Road-Kafka Adapter

## Using Docker

You can create a docker image to run X-Road-Kafka Adapter inside a container, using the provided Dockerfile. Before building the image, build the jar file inside `src` directory:

```
mvn clean install
```

If you have not built the jar, building the Docker image will fail with message:

```
Step 2 : ADD target/xrd-kafka-adapter-*.jar xrd-kafka-adapter.jar
No source files were specified
```

While you are in the project root directory, build the image using the Docker `build` command. The `-t` parameter gives your image a tag, so you can run it more easily later. Don’t forget the `.` parameter, which tells the Docker `build` command to look in the current directory for a file called `Dockerfile`.

```
docker build -t xrd-kafka-adapter .
```

After building the image, you can run X-Road-Kafka Adapter using it.

```
docker run -p 8080:8080 xrd-kafka-adapter
```

If customized properties are used, the host directory containing the properties files must be mounted as a data directory. In addition, the directory containing the properties files inside the container must be set using JAVA_OPTS and propertiesDirectory property.

The Kafka REST proxy URL can be defined using the `app.kafka.broker-url=<PROXY_URL>` property. Also, X-Road service identifier can be mapped to Kafka topic using the `app.service-id-topic-mapping.<serviceIdentifier>=<KAFKA_TOPIC_NAME>` property.

```
docker run -p 8080:8080 -e "JAVA_OPTS=-Dapp.kafka.broker-url=http://rest-proxy:8082 -Dapp.service-id-topic-mapping.CS/ORG/1111/TestService/ServiceCode=MyTopic" xrd-kafka-adapter
```
