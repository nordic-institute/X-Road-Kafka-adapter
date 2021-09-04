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

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;

/**
 * Test cases for HelperService class.
 */
@RunWith(MockitoJUnitRunner.class)
public class HelperServiceTest extends TestCase {

    private static final String PROXY_URL = "http://localhost:8080";

    private static final String BROKER_ADDRESS = "localhost:9092";

    private HelperService helperService;

    @Mock
    private Environment environment;

    @Before
    public void setup() {
        helperService = new HelperService(environment);
        Mockito.when(environment.getProperty(Constants.KAFKA_REST_PROXY_URL_PROPERTY_KEY)).thenReturn(PROXY_URL);
        Mockito.when(environment.getProperty(Constants.KAFKA_BROKER_ADDRESS_PROPERTY_KEY)).thenReturn(BROKER_ADDRESS);
    }

    @Test
    public void testGetKafkaBrokerAddress() {
        String address = helperService.getKafkaBrokerAddress();
        Assert.assertEquals(BROKER_ADDRESS, address);
    }

    @Test
    public void testGetKafkaRESTProxyUrl() {
        String url = helperService.getKafkaRESTProxyUrl();
        Assert.assertEquals(PROXY_URL, url);
    }

    @Test
    public void testGetKafkaConsumerGroupName() {
        String expected = "PLAYGROUND_ORG_2908758-4_TestClient_MyTopic" + Constants.KAFKA_CONSUMER_GROUP_POSTFIX;
        String clientId = "PLAYGROUND/ORG/2908758-4/TestClient";
        String topic = "MyTopic";
        String consumerGroupName = helperService.getKafkaConsumerGroupName(clientId, topic);
        Assert.assertEquals(expected, consumerGroupName);
    }

    @Test
    public void testGetKafkaConsumerInstanceName() {
        String expected = "PLAYGROUND_COM_1234567-8_Client" + Constants.KAFKA_CONSUMER_INSTANCE_POSTFIX;
        String clientId = "PLAYGROUND/COM/1234567-8/Client";
        String consumerInstanceName = helperService.getKafkaConsumerInstanceName(clientId);
        Assert.assertEquals(expected, consumerInstanceName);
    }

    @Test
    public void testWrapErrorMessageInJson() {
        String json = helperService.wrapErrorMessageInJson(HttpStatus.GATEWAY_TIMEOUT.value(), "Error message");
        Assert.assertEquals("{\"error_code\":504,\"message\":\"Error message\"}", json);
    }
}
