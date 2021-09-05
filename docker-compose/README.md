# Docker Compose

This Docker Compose file consists of the following containers:

- Zookeeper
- Kafka broker
- Schema registry
- REST proxy
- Standalone Security Server
- X-Road-Kafka Adapter

The Docker Compose file is a modified version of the Docker Compose [example](https://raw.githubusercontent.com/confluentinc/cp-all-in-one/6.2.0-post/cp-all-in-one/docker-compose.yml) 
provided by Confluent.

Before running the stack, please build the `xrd-kafka-adapter` Docker image locally. The required steps are explained 
[here](../README.md#using-docker).

Then, run the stack:
```
docker-compose up -d
```

View `xrd-kafka-adapter` logs:
```
docker-compose logs -f xrd-kafka-adapter
```

## Initial Configuration

One the stack is up and running, some initial configuration steps are required.

- Get the Kafka cluster ID.

```
KAFKA_CLUSTER_ID=$(docker-compose exec rest-proxy curl -X GET \
     "http://localhost:8082/v3/clusters/" | jq -r ".data[0].cluster_id")
```

- Create a new Kafka topic, e.g., `MyTopic`.

```
docker-compose exec rest-proxy curl -X POST \
     -H "Content-Type: application/json" \
     -d "{\"topic_name\":\"MyTopic\",\"partitions_count\":6,\"configs\":[]}" \
     "http://localhost:8082/v3/clusters/${KAFKA_CLUSTER_ID}/topics" | jq .
```

- Open the Standalone Security Server UI (`https://localhost:4000`) and login. Username is `xrd` and password is `secret`.

- Go to the Clients view, open the `CS:ORG:1111:TestService` client and go to the Services tab.

- Click the Add REST button and create a new `OPENAPI` service using the following information:
  - URL type: `OpenAPI3 Description`
  - URL: `http://xrd-kafka-adapter:8080/api/v1/MyTopic/openapi-definition.yaml`
  - Service code: `MyTopic`

- Click save and enable the new service.

- Open the service code details by clicking the service code (`MyTopic`).

- In the Service URL field, replace the `<KAFKA_TOPIC_NAME>` with the name of the new topic that was created in step 2 and click Save.
  - `http://xrd-kafka-adapter:8080/api/v1/<KAFKA_TOPIC_NAME>` => `http://xrd-kafka-adapter:8080/api/v1/MyTopic`
  
- Give the `CS:ORG:1111:TestClient` client access rights to the service on the service code level.

## Producing and Consuming Data

After completing the initial configuration, you can use the following `curl` commands to access the topic through the Security Server.

- Publish data to `MyTopic`.

  - Without a key.
    ```
    curl -X POST -H 'X-Road-Client: CS/ORG/1111/TestClient' -H 'Content-Type: application/json' -i 'http://localhost/r1/CS/ORG/1111/TestService/MyTopic/records' --data '{"records":[{"value":{"field1":"value1", "field2":"value2"}}]}'
    ```

  - With a key.
    ```
    curl -X POST -H 'X-Road-Client: CS/ORG/1111/TestClient' -H 'Content-Type: application/json' -i 'http://localhost/r1/CS/ORG/1111/TestService/MyTopic/records' --data '{"records":[{"key":"test", "value":{"field1":"value1", "field2":"value2"}}]}'
    ```
  
- Subscribe to `MyTopic`.

  - With default offset reset policy (`earliest`).
    ```
    curl -X POST -H 'X-Road-Client: CS/ORG/1111/TestClient' -i 'http://localhost/r1/CS/ORG/1111/TestService/MyTopic/subscriptions'
    ```
 
  - With the offset reset policy defined in the request (`earliest` / `latest`).
    ```
    curl -X POST -H 'X-Road-Client: CS/ORG/1111/TestClient' -i 'http://localhost/r1/CS/ORG/1111/TestService/MyTopic/subscriptions?offsetResetPolicy=latest'
    ```
      
- Read data from `MyTopic`.
```
curl -X GET -H 'X-Road-Client: CS/ORG/1111/TestClient' -i 'http://localhost/r1/CS/ORG/1111/TestService/MyTopic/records'
```

- Unsubscribe from `MyTopic`.
```
curl -X DELETE -H 'X-Road-Client: CS/ORG/1111/TestClient' -i 'http://localhost/r1/CS/ORG/1111/TestService/MyTopic/subscriptions'
```