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
package org.niis.xrdkafkaadapter.service;

import org.niis.xrdkafkaadapter.util.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * This class provides helper services to other classes.
 *
 */
@Service
public class HelperService {

    private static final Logger LOG = LoggerFactory.getLogger(HelperService.class);

    private static final int SERVICE_CODE_INDEX = 4;

    @Autowired
    private Environment env;

    /**
     * Reads Kafka broker URL configuration property value. If the property hasn't been set, null is returned.
     * @return Kafka broker URL or null
     */
    public String getKafkaBrokerUrl() {
        return env.getProperty(Constants.KAFKA_BROKER_URL_PROPERTY_KEY);
    }

    /**
     * Reads Kafka REST proxy URL configuration property value. If the property hasn't been set, null is returned.
     * @return Kafka REST proxy URL or null
     */
    public String getKafkaRESTProxyUrl() {
        return env.getProperty(Constants.KAFKA_REST_PROXY_URL_PROPERTY_KEY);
    }

    /**
     * Converts X-Road client identifier to Kafka consumer group name using the following pattern:
     * "<instanceIdentifier>/<memberClass>/<memberCode>/<subsystemCode>"
     * =>
     * "<instanceIdentifier>_<memberClass>_<memberCode>_<subsystemCode>_<topicName>_group"
     * @param xrdClientId X-Road client identifier
     * @param topicName Kafka topic name
     * @return X-Road client identifier converted to Kafka consumer group name
     */
    public String getKafkaConsumerGroupName(String xrdClientId, String topicName) {
        return xrdClientId.replaceAll("\\/", "_") + "_" + topicName + Constants.KAFKA_CONSUMER_GROUP_POSTFIX;
    }

    /**
     * Converts X-Road client identifier to Kafka consumer instance name using the following pattern:
     * "<instanceIdentifier>/<memberClass>/<memberCode>/<subsystemCode>"
     * =>
     * "<instanceIdentifier>_<memberClass>_<memberCode>_<subsystemCode>_instance"
     * @param xrdClientId X-Road client identifier
     * @return X-Road client identifier converted to Kafka consumer instance name
     */
    public String getKafkaConsumerInstanceName(String xrdClientId) {
        return xrdClientId.replaceAll("\\/", "_") + Constants.KAFKA_CONSUMER_INSTANCE_POSTFIX;
    }

    private String getServiceIdTopicMapping(String serviceId) {
        return env.getProperty(Constants.SERVICE_ID_TOPIC_MAPPING_PROPERTY_KEY + serviceId);
    }
}
