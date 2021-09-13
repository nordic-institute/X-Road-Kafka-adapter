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

import org.niis.xrdkafkaadapter.exception.BadRequestException;
import org.niis.xrdkafkaadapter.exception.ForbiddenRequestException;
import org.niis.xrdkafkaadapter.exception.RequestFailedException;
import org.niis.xrdkafkaadapter.model.KafkaClientResponse;
import org.niis.xrdkafkaadapter.model.OffsetResetPolicy;
import org.niis.xrdkafkaadapter.service.HelperService;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * This class implements a TCP client for Kafka.
 */
@Service
public class TcpClient implements KafkaClient {

    private static final Logger LOG = LoggerFactory.getLogger(TcpClient.class);

    private static final int POLL_TIMEOUT_MS = 100;

    // Default (can be overridden in props): 600s = 10min
    private static final int CONSUMER_CACHE_DURATION_S = 600;

    // 60000ms = 1min
    private static final int CONSUMER_CACHE_CLEAN_UP_INTERVAL_MS = 60000;

    // 10000ms = 10s
    private static final int CONSUMER_CACHE_CLEAN_UP_INITIAL_DELAY_MS = 10000;

    private static final String ENABLE_AUTO_COMMIT = "true";

    private static final String AUTO_COMMIT_INTERVAL_MS = "1000";

    private static final String STRING_DESERIALIZER_CLASS = "org.apache.kafka.common.serialization.StringDeserializer";

    private static final String STRING_SERIALIZER_CLASS = "org.apache.kafka.common.serialization.StringSerializer";

    private static final String ERROR_IN_SENDING_RECORD = "Error in sending record";

    private static final String VALUE_MISSING_ERROR = "Invalid record. Value is missing.";

    private static final String NO_SUBSCRIPTION_FOUND_ERROR = "No subscription found.";

    @Autowired
    private HelperService helperService;

    private LoadingCache<String, KafkaConsumer> consumerCache;

    /**
     * Initialize new TcpClient object.
     *
     * @param helperService
     */
    public TcpClient(HelperService helperService) {
        this.helperService = helperService;
        int cacheDuration = helperService.getCacheDuration(CONSUMER_CACHE_DURATION_S);

        LOG.debug("Cache duration is {}s", cacheDuration);
        LOG.debug("Cache clean up initial delay is {}ms", CONSUMER_CACHE_CLEAN_UP_INITIAL_DELAY_MS);
        LOG.debug("Cache clean up interval is {}ms", CONSUMER_CACHE_CLEAN_UP_INTERVAL_MS);

        /**
         * The "expireAfterAccess" specifies that each entry should be automatically removed from the cache once a fixed
         * duration has elapsed after the entry's creation, the most recent replacement of its value, or its
         * last access. Access time is reset by all cache read and write operations (including Cache.asMap().get(Object)
         * and Cache.asMap().put(K, V)), but not by operations on the collection-views of Cache.asMap().
         *
         * The removalListener method is not called for objects that are automatically removed from cache
         * because they have expired. For those objects, the removalListener method is invoked only when the cache
         * cleanUp() method is invoked. Therefore, the cleanUp() is scheduled to be invoked once in a minute. In addition,
         * cleanUp() is explicitly invoked in subscribe method.
         *
         * When cleanUp() is invoked, the connections of all consumers that have been removed from the cache because of
         * expiration since cleanUp() was invoked the last time, are closed. In other words, when a consumer expires
         * in the cache, the connection is not closed until cleanUp() is invoked.
         */
        consumerCache = CacheBuilder.newBuilder()
                .expireAfterAccess(cacheDuration, TimeUnit.SECONDS)
                // N.B. Not invoked automatically when entry expires
                .removalListener((RemovalListener<String, KafkaConsumer>) entry -> {
                    KafkaConsumer consumer = entry.getValue();
                    LOG.debug("Remove consumer \"{}\" from consumer cache", entry.getKey());
                    if (consumer != null) {
                        try {
                            // Close connection
                            consumer.close();
                            LOG.debug("Connection closed for consumer object \"{}\"", consumer);
                        } catch (Throwable e) {
                            LOG.error("Failed to close Kafka consumer: {}", consumer.getClass().getName(), e);
                        }
                    }
                })
                .build(new CacheLoader<String, KafkaConsumer>() {
                    @Override
                    public KafkaConsumer load(String key) throws ForbiddenRequestException {
                        throw new ForbiddenRequestException(NO_SUBSCRIPTION_FOUND_ERROR);
                    }
                });
    }

    /**
     * Perform any pending maintenance operations for consumer cache, e.g., run "removalListener" for expired
     * cache entries.
     */
    @Scheduled(fixedRate = CONSUMER_CACHE_CLEAN_UP_INTERVAL_MS, initialDelay = CONSUMER_CACHE_CLEAN_UP_INITIAL_DELAY_MS)
    protected void cleanUpCache() {
        LOG.debug("Clean up consumer cache");
        consumerCache.cleanUp();
    }

    /**
     * Subscribe to Kafka topic.
     *
     * @param xrdClientId
     * @param topicName
     * @param offsetResetPolicy
     * @return
     * @throws RequestFailedException
     */
    public KafkaClientResponse subscribe(String xrdClientId, String topicName, OffsetResetPolicy offsetResetPolicy)
            throws RequestFailedException {
        String groupName = helperService.getKafkaConsumerGroupName(xrdClientId, topicName);

        // Check if the consumer already exists in the cache and create a new one if it doesn't
        if (!consumerCache.asMap().containsKey(groupName)) {
            // Clean up consumer cache in case this consumer has a previous expired consumer instance that has been
            // removed from cache, but the connection hasn't been closed yet.
            cleanUpCache();
            LOG.debug("Add new consumer \"{}\" to consumer cache", groupName);
            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(getConsumerProperties(xrdClientId, topicName, offsetResetPolicy));
            consumerCache.asMap().put(groupName, consumer);
            LOG.debug("Consumer cache size: {}", consumerCache.size());
        }
        // Subscribe to the topic
        consumerCache.asMap().get(groupName).subscribe(Arrays.asList(topicName));

        return new KafkaClientResponse();
    }

    /**
     *  Unsubscribe from a Kafka topic.
     *
     * @param xrdClientId
     * @param topicName
     * @return
     * @throws RequestFailedException
     */
    public KafkaClientResponse unsubscribe(String xrdClientId, String topicName) throws RequestFailedException, ForbiddenRequestException {
        String groupName = helperService.getKafkaConsumerGroupName(xrdClientId, topicName);

        if (consumerCache.asMap().containsKey(groupName)) {
            // The connection is closed in the removalListener - no need to close it here
            // Remove consumer from cache
            consumerCache.invalidate(groupName);
            LOG.debug("Consumer cache size: {}", consumerCache.size());
            return new KafkaClientResponse();
        }
        LOG.debug("Unable to unsubscribe from topic - no subscription found");
        throw new ForbiddenRequestException(NO_SUBSCRIPTION_FOUND_ERROR);
    }

    /**
     *  Consumer data from Kafka topic.
     *
     * @param xrdClientId
     * @param topicName
     * @return
     * @throws RequestFailedException
     */
    public KafkaClientResponse read(String xrdClientId, String topicName) throws RequestFailedException, ForbiddenRequestException {
        String groupName = helperService.getKafkaConsumerGroupName(xrdClientId, topicName);
        if (consumerCache.asMap().containsKey(groupName)) {
            ConsumerRecords<String, String> records = consumerCache.asMap().get(groupName).poll(Duration.ofMillis(POLL_TIMEOUT_MS));
            LOG.debug("Received {} records from the topic", records.count());

            // JSON object for the response
            JSONArray response = new JSONArray();

            records.forEach(record -> {
                response.put(generateReadResultsEntry(record.partition(), record.offset(), topicName, record.key(), record.value()));
            });
            return new KafkaClientResponse(response.toString());
        }
        LOG.debug("Unable to read topic - no subscription found");
        throw new ForbiddenRequestException(NO_SUBSCRIPTION_FOUND_ERROR);
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
        Producer<String, String> producer = new KafkaProducer<>(getProducerProperties(xrdClientId, topicName));

        // JSON object for the response
        JSONObject response = new JSONObject();
        JSONArray offsets = new JSONArray();
        response.put("offsets", offsets);
        JSONObject json = null;

        try {
            json = new JSONObject(messageBody);
        } catch (JSONException je) {
            throw new BadRequestException("Invalid JSON object in request body");
        }

        LOG.debug("Request records count is {}", json.getJSONArray("records").length());

        json.getJSONArray("records").forEach(element -> {
            JSONObject offsetEntry = null;
            try {
                // Key is optional
                String key = null;
                if (!((JSONObject)element).isNull("key")) {
                    key = ((JSONObject)element).getString("key");
                }
                // Value is mandatory
                String value = ((JSONObject)element).get("value").toString();

                // Create new record and send it
                ProducerRecord<String, String> record = getProducerRecord(topicName, key, value);
                RecordMetadata metadata = producer.send(record).get();
                LOG.debug("Record sent to partition {} with offset {}", metadata.partition(), metadata.offset());

                offsetEntry = generatePublishResultsEntry(metadata, null);
            } catch (ExecutionException | InterruptedException e) {
                LOG.error(ERROR_IN_SENDING_RECORD);
                LOG.error(e.getMessage(), e);
                offsetEntry = generatePublishResultsEntry(null, ERROR_IN_SENDING_RECORD);
            } catch (JSONException je) {
                LOG.error(VALUE_MISSING_ERROR);
                offsetEntry = generatePublishResultsEntry(null, VALUE_MISSING_ERROR);
            }
            offsets.put(offsetEntry);
        });
        producer.close();
        return new KafkaClientResponse(response.toString());
    }

    protected JSONObject generateReadResultsEntry(int partition, long offset, String topic, String key, String value) {
        JSONObject entry = new JSONObject();
        entry.put("partition", partition);
        entry.put("offset", offset);
        entry.put("topic", topic);
        entry.put("key", key);
        try {
            entry.put("value", new JSONObject(value));
        } catch (JSONException je) {
            entry.put("value", value);
        }
        return entry;
    }

    protected JSONObject generatePublishResultsEntry(RecordMetadata metadata, String errorMsg) {
        JSONObject entry = new JSONObject();
        if (metadata != null) {
            entry.put("partition", metadata.partition());
            entry.put("offset", metadata.offset());
            entry.put("success", true);
        } else {
            entry.put("success", false);
            entry.put("error_message", errorMsg);
        }
        return entry;
    }

    protected Properties getConsumerProperties(String xrdClientId, String topicName, OffsetResetPolicy offsetResetPolicy) {
        Properties props = new Properties();
        props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, helperService.getKafkaBrokerAddress());
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, helperService.getKafkaConsumerGroupName(xrdClientId, topicName));
        props.setProperty(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, helperService.getKafkaConsumerInstanceName(xrdClientId));
        props.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, ENABLE_AUTO_COMMIT);
        props.setProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, AUTO_COMMIT_INTERVAL_MS);
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, STRING_DESERIALIZER_CLASS);
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, STRING_DESERIALIZER_CLASS);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetResetPolicy.toString().toLowerCase());
        return props;
    }

    protected Properties getProducerProperties(String xrdClientId, String topicName) {
        Properties props = new Properties();
        props.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, helperService.getKafkaBrokerAddress());
        props.setProperty(ProducerConfig.CLIENT_ID_CONFIG, helperService.getKafkaProducerClientId(xrdClientId, topicName));
        props.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, STRING_SERIALIZER_CLASS);
        props.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, STRING_SERIALIZER_CLASS);
        return props;
    }

    protected ProducerRecord<String, String> getProducerRecord(String topicName, String key, String value) {
        if (key != null) {
            return new ProducerRecord<String, String>(topicName, key, value);
        }
        return new ProducerRecord<String, String>(topicName, value);
    }
}
