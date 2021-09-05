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

import org.niis.xrdkafkaadapter.model.OffsetResetPolicy;
import org.niis.xrdkafkaadapter.service.HelperService;
import org.niis.xrdkafkaadapter.util.Constants;

import junit.framework.TestCase;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;

import java.util.Properties;

/**
 * Test cases for RESTProxyClient class.
 */
@RunWith(MockitoJUnitRunner.class)
public class TcpClientTest extends TestCase {

    private static final String BROKER_ADDRESS = "localhost:9092";

    private TcpClient tcpClient;

    private HelperService helperService;

    @Mock
    private Environment environment;

    @Before
    public void setup() {
        helperService = new HelperService(environment);
        tcpClient = new TcpClient(helperService);
        Mockito.when(environment.getProperty(Constants.KAFKA_BROKER_ADDRESS_PROPERTY_KEY)).thenReturn(BROKER_ADDRESS);
    }

    @Test
    public void testGeneratePublishResultsEntrySuccess() {
        TopicPartition tp = new TopicPartition("MyTopic", 0);
        RecordMetadata metadata = new RecordMetadata(tp, 1, 2, 3, 4L, 5, 6);
        JSONObject json = tcpClient.generatePublishResultsEntry(metadata, null);
        Assert.assertEquals(0, json.getInt("partition"));
        Assert.assertEquals(3, json.getInt("offset"));
        Assert.assertEquals(true, json.getBoolean("success"));
        Assert.assertEquals(false, json.has("error_message"));
    }

    @Test
    public void testGeneratePublishResultsEntryError() {
        JSONObject json = tcpClient.generatePublishResultsEntry(null, "error message");
        Assert.assertEquals(false, json.has("partition"));
        Assert.assertEquals(false, json.has("offset"));
        Assert.assertEquals(false, json.getBoolean("success"));
        Assert.assertEquals("error message", json.getString("error_message"));
    }

    @Test
    public void testGenerateReadResultsEntryString() {
        JSONObject json = tcpClient.generateReadResultsEntry(1, 2, "TestTopic", "key", "string value");
        Assert.assertEquals(1, json.getInt("partition"));
        Assert.assertEquals(2, json.getInt("offset"));
        Assert.assertEquals("TestTopic", json.getString("topic"));
        Assert.assertEquals("key", json.getString("key"));
        Assert.assertEquals("string value", json.getString("value"));
    }

    @Test
    public void testGenerateReadResultsEntryObject() {
        JSONObject json = tcpClient.generateReadResultsEntry(5, 22, "TestTopic", "my_key", "{\"field1\":\"value1\"}");
        Assert.assertEquals(5, json.getInt("partition"));
        Assert.assertEquals(22, json.getInt("offset"));
        Assert.assertEquals("TestTopic", json.getString("topic"));
        Assert.assertEquals("my_key", json.getString("key"));
        Assert.assertEquals("{\"field1\":\"value1\"}", json.getJSONObject("value").toString());
    }

    @Test
    public void testGetConsumerProperties() {
        Properties props = tcpClient.getConsumerProperties("PLAYGROUND/COM/1234567-8/Client", "TestTopic", OffsetResetPolicy.LATEST);
        Assert.assertEquals(BROKER_ADDRESS, props.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        Assert.assertEquals("PLAYGROUND_COM_1234567-8_Client_TestTopic_group", props.getProperty(ConsumerConfig.GROUP_ID_CONFIG));
        Assert.assertEquals("PLAYGROUND_COM_1234567-8_Client_instance", props.getProperty(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG));
        Assert.assertEquals("true", props.getProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG));
        Assert.assertEquals("1000", props.getProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG));
        Assert.assertEquals("org.apache.kafka.common.serialization.StringDeserializer", props.getProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG));
        Assert.assertEquals("org.apache.kafka.common.serialization.StringDeserializer", props.getProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG));
        Assert.assertEquals(OffsetResetPolicy.LATEST.toString().toLowerCase(), props.getProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
    }

    @Test
    public void testGetProducerProperties() {
        Properties props = tcpClient.getProducerProperties("PLAYGROUND/COM/1234567-8/Client", "TestTopic");
        Assert.assertEquals(BROKER_ADDRESS, props.getProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
        Assert.assertEquals("PLAYGROUND_COM_1234567-8_Client_TestTopic_producer", props.getProperty(ProducerConfig.CLIENT_ID_CONFIG));
        Assert.assertEquals("org.apache.kafka.common.serialization.StringSerializer", props.getProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG));
        Assert.assertEquals("org.apache.kafka.common.serialization.StringSerializer", props.getProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG));
    }
}
