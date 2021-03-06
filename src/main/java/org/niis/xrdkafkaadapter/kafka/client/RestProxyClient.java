/*
 * The MIT License
 *
 * Copyright (c) 2021 Nordic Institute for Interoperability Solutions (NIIS)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xrdkafkaadapter.kafka.client;

import org.niis.xrd4j.rest.ClientResponse;
import org.niis.xrd4j.rest.client.RESTClient;
import org.niis.xrd4j.rest.client.RESTClientFactory;
import org.niis.xrdkafkaadapter.exception.BadRequestException;
import org.niis.xrdkafkaadapter.exception.ForbiddenRequestException;
import org.niis.xrdkafkaadapter.exception.RequestFailedException;
import org.niis.xrdkafkaadapter.model.KafkaClientResponse;
import org.niis.xrdkafkaadapter.model.OffsetResetPolicy;
import org.niis.xrdkafkaadapter.service.HelperService;
import org.niis.xrdkafkaadapter.util.Constants;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * This class implements a HTTP client for Kafka REST Proxy.
 */
@Service
public class RestProxyClient implements KafkaClient {

    private static final Logger LOG = LoggerFactory.getLogger(RestProxyClient.class);

    private static final String REQUEST_FAILED_ERROR_MESSAGE = "Sending request to REST Proxy failed.";

    private static final String NO_SUBSCRIPTION_FOUND_ERROR = "No subscription found.";

    private static final String CONSUMERS_PATH = "/consumers/";

    private static final String INSTANCES_PATH = "/instances/";

    private static final String SUBSCRIPTION_PATH = "/subscription";

    private static final String RECORDS_PATH = "/records";

    private static final String TOPICS_PATH = "/topics/";

    @Autowired
    private HelperService helperService;

    /**
     * Initialize new RESTProxyClient object.
     */
    public RestProxyClient() { }

    /**
     * Initialize new RESTProxyClient object.
     *
     * @param helperService
     */
    public RestProxyClient(HelperService helperService) {
        this.helperService = helperService;
    }

    /**
     * Subscribe to Kafka topic. Subscription consists of two separate operations:
     *
     * 1. Create a new consumer instance in the consumer group.
     * 2. Subscribe to the given topic to get dynamically assigned partitions. If a prior subscription exists, it is
     * be replaced by the latest subscription.
     *
     * @param xrdClientId
     * @param topicName
     * @param offsetResetPolicy
     * @return
     * @throws RequestFailedException
     */
    public KafkaClientResponse subscribe(String xrdClientId, String topicName, OffsetResetPolicy offsetResetPolicy)
            throws RequestFailedException {
        // Generate Kafka consumer group and consumer instance names
        String groupName = helperService.getKafkaConsumerGroupName(xrdClientId, topicName);
        String instanceName = helperService.getKafkaConsumerInstanceName(xrdClientId);

        // Create request object and request target URL
        JSONObject createConsumerInstanceRequest = buildCreateConsumerInstanceRequest(instanceName, offsetResetPolicy);
        LOG.debug("Consumer instance request: {}", createConsumerInstanceRequest.toString());
        String consumerGroupUrl = buildConsumerGroupUrl(groupName);

        // Create REST client
        RESTClient restClient = RESTClientFactory.createRESTClient(HttpMethod.POST.toString());
        Map<String, String> params = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.HTTP_HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_KAFKA_JSON_V2);

        // Send create consumer instance request
        ClientResponse restResponse = restClient.send(consumerGroupUrl, createConsumerInstanceRequest.toString(), params, headers);
        // If the request failed, the response is null
        if (restResponse == null) {
            throw new RequestFailedException(REQUEST_FAILED_ERROR_MESSAGE);
        }

        // Status codes 200 (OK) and 409 (Conflict) can be ignored. 409 means that consumer instance with the specified
        // name already exists. In case of other status code, return the response.
        if (restResponse.getStatusCode() != HttpStatus.SC_OK
                && restResponse.getStatusCode() != HttpStatus.SC_CONFLICT) {
            LOG.debug("Unable to subscribe to a topic. Status code {} detected.", restResponse.getStatusCode());
            return new KafkaClientResponse(restResponse.getData());
        }

        // Create request object and request target URL
        JSONObject subscribeToTopicRequest = buildSubscribeToTopicRequest(topicName);
        LOG.debug("Subscribe to topic request: {}", subscribeToTopicRequest.toString());
        String subscriptionsUrl = buildSubscriptionsUrl(groupName, instanceName);

        // Send subscribe to topic request
        restResponse = restClient.send(subscriptionsUrl, subscribeToTopicRequest.toString(), params, headers);
        // If the request failed, the response is null
        if (restResponse == null) {
            throw new RequestFailedException(REQUEST_FAILED_ERROR_MESSAGE);
        }
        return new KafkaClientResponse(restResponse.getData());
    }

    /**
     * Unsubscribe from a Kafka topic. Unsubscribing from topic consists of two separate operations:
     *
     * 1. Unsubscribe from a topic.
     * 2. Destroy the consumer instance.
     *
     * @param xrdClientId
     * @param topicName
     * @return
     * @throws RequestFailedException
     */
    public KafkaClientResponse unsubscribe(String xrdClientId, String topicName) throws RequestFailedException, ForbiddenRequestException {
        // Generate Kafka consumer group and consumer instance names
        String groupName = helperService.getKafkaConsumerGroupName(xrdClientId, topicName);
        String instanceName = helperService.getKafkaConsumerInstanceName(xrdClientId);

        // Create request target URL
        String subscriptionsUrl = buildSubscriptionsUrl(groupName, instanceName);

        // Create REST client
        RESTClient restClient = RESTClientFactory.createRESTClient(HttpMethod.DELETE.toString());
        Map<String, String> params = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.HTTP_HEADER_ACCEPT, Constants.CONTENT_TYPE_KAFKA_JSON_V2);

        // Send unsubscribe from a topic request
        ClientResponse restResponse = restClient.send(subscriptionsUrl, null, params, headers);
        // If the request failed, the response is null
        if (restResponse == null) {
            throw new RequestFailedException(REQUEST_FAILED_ERROR_MESSAGE);
        }

        // Status code 204 (No content) can be ignored. In case of other status code, return the response.
        if (restResponse.getStatusCode() != HttpStatus.SC_NO_CONTENT) {
            LOG.debug("Unable to unsubscribe from a topic. Status code {} detected.", restResponse.getStatusCode());
            // Most common reason is that subscription doesn't exist
            throw new ForbiddenRequestException(NO_SUBSCRIPTION_FOUND_ERROR);
        }

        // Create request object and request target URL
        String consumerGroupInstanceUrl = buildConsumerGroupInstanceUrl(groupName, instanceName);

        // Send destroy a consumer instance request
        restResponse = restClient.send(consumerGroupInstanceUrl, null, params, headers);
        // If the request failed, the response is null
        if (restResponse == null) {
            throw new RequestFailedException(REQUEST_FAILED_ERROR_MESSAGE);
        }
        return new KafkaClientResponse();
    }

    /**
     * Consumer data from Kafka topic.
     *
     * @param xrdClientId
     * @param topicName
     * @return
     * @throws RequestFailedException
     */
    public KafkaClientResponse read(String xrdClientId, String topicName) throws RequestFailedException, ForbiddenRequestException {
        // Generate Kafka consumer group and consumer instance names
        String groupName = helperService.getKafkaConsumerGroupName(xrdClientId, topicName);
        String instanceName = helperService.getKafkaConsumerInstanceName(xrdClientId);

        // Create request target URL
        String consumerInstanceRecordsUrl = buildConsumerInstanceRecordsUrl(groupName, instanceName);

        // Create REST client
        RESTClient restClient = RESTClientFactory.createRESTClient(HttpMethod.GET.toString());
        Map<String, String> params = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.HTTP_HEADER_ACCEPT, Constants.CONTENT_TYPE_KAFKA_JSON_V2);

        // Send read from topic request
        ClientResponse restResponse = restClient.send(consumerInstanceRecordsUrl, null, params, headers);
        // If the request failed, the response is null
        if (restResponse == null) {
            throw new RequestFailedException(REQUEST_FAILED_ERROR_MESSAGE);
        }
        return new KafkaClientResponse(restResponse.getData());
    }

    /**
     * Publish data to a Kafka topic.
     *
     * @param xrdClientId
     * @param topicName
     * @param messageBody
     * @return
     * @throws RequestFailedException
     */
    public KafkaClientResponse publish(String xrdClientId, String topicName, String messageBody)
            throws RequestFailedException, BadRequestException {
        // Create request target URL
        String topicsUrl = buildTopicUrl(topicName);

        // Create REST client
        RESTClient restClient = RESTClientFactory.createRESTClient(HttpMethod.POST.toString());
        Map<String, String> params = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.HTTP_HEADER_CONTENT_TYPE, Constants.CONTENT_TYPE_KAFKA_JSON_V2);

        // Send read from topic request
        ClientResponse restResponse = restClient.send(topicsUrl, messageBody, params, headers);
        // If the request failed, the response is null
        if (restResponse == null) {
            throw new RequestFailedException(REQUEST_FAILED_ERROR_MESSAGE);
        }
        return new KafkaClientResponse(restResponse.getData());
    }

    protected JSONObject buildCreateConsumerInstanceRequest(String instanceName, OffsetResetPolicy offsetResetPolicy) {
        JSONObject json = new JSONObject();
        json.put("name", instanceName);
        json.put("format", "json");
        json.put("auto.offset.reset", offsetResetPolicy.toString().toLowerCase()); // earliest | latest
        return json;
    }

    protected JSONObject buildSubscribeToTopicRequest(String topicName) {
        JSONArray topics = new JSONArray();
        topics.put(topicName);
        JSONObject json = new JSONObject();
        json.put("topics", topics);
        return json;
    }

    protected String buildConsumerGroupUrl(String consumerGroupName) {
        StringBuilder sb = new StringBuilder();
        sb.append(helperService.getKafkaRESTProxyUrl()).append(CONSUMERS_PATH).append(consumerGroupName);
        return sb.toString();
    }

    protected String buildConsumerGroupInstanceUrl(String consumerGroupName, String consumerInstanceName) {
        String consumerGroupUrl = buildConsumerGroupUrl(consumerGroupName);
        StringBuilder sb = new StringBuilder(consumerGroupUrl);
        sb.append(INSTANCES_PATH).append(consumerInstanceName);
        return sb.toString();
    }

    protected String buildSubscriptionsUrl(String consumerGroupName, String consumerInstanceName) {
        String consumerGroupInstanceUrl = buildConsumerGroupInstanceUrl(consumerGroupName, consumerInstanceName);
        StringBuilder sb = new StringBuilder(consumerGroupInstanceUrl);
        sb.append(SUBSCRIPTION_PATH);
        return sb.toString();
    }

    protected String buildConsumerInstanceRecordsUrl(String consumerGroupName, String consumerInstanceName) {
        String consumerGroupInstanceUrl = buildConsumerGroupInstanceUrl(consumerGroupName, consumerInstanceName);
        StringBuilder sb = new StringBuilder(consumerGroupInstanceUrl);
        sb.append(RECORDS_PATH);
        return sb.toString();
    }

    protected String buildTopicUrl(String topicName) {
        StringBuilder sb = new StringBuilder();
        sb.append(helperService.getKafkaRESTProxyUrl()).append(TOPICS_PATH).append(topicName);
        return sb.toString();
    }
}
