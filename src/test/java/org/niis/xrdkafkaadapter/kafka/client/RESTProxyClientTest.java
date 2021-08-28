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
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;
/**
 * Test cases for RESTProxyClient class.
 */
@RunWith(MockitoJUnitRunner.class)
public class RESTProxyClientTest extends TestCase {

    private static final String BASE_URL = "http://localhost:8080";

    private RESTProxyClient restProxyClient;

    private HelperService helperService;

    @Mock
    private Environment environment;

    @Before
    public void setup() {
        helperService = new HelperService(environment);
        restProxyClient = new RESTProxyClient(helperService);
        Mockito.when(environment.getProperty(Constants.KAFKA_REST_PROXY_URL_PROPERTY_KEY)).thenReturn(BASE_URL);
    }

    @Test
    public void testBuildCreateConsumerInstanceRequest() {
        JSONObject json = restProxyClient.buildCreateConsumerInstanceRequest("instanceName", OffsetResetPolicy.LATEST);
        Assert.assertEquals("instanceName", json.getString("name"));
        Assert.assertEquals("json", json.getString("format"));
        Assert.assertEquals("latest", json.getString("auto.offset.reset"));
        Assert.assertEquals("{\"name\":\"instanceName\",\"format\":\"json\",\"auto.offset.reset\":\"latest\"}", json.toString());
    }

    @Test
    public void testBuildSubscribeToTopicRequest() {
        JSONObject json = restProxyClient.buildSubscribeToTopicRequest("MyTopic");
        Assert.assertEquals("MyTopic", json.getJSONArray("topics").getString(0));
        Assert.assertEquals("{\"topics\":[\"MyTopic\"]}", json.toString());
    }

    @Test
    public void testBuildConsumerGroupUrl() {
        String url = restProxyClient.buildConsumerGroupUrl("consumer_group");
        Assert.assertEquals("http://localhost:8080/consumers/consumer_group", url);
    }

    @Test
    public void testBuildConsumerGroupInstanceUrl() {
        String url = restProxyClient.buildConsumerGroupInstanceUrl("consumer_group", "consumer_instance");
        Assert.assertEquals("http://localhost:8080/consumers/consumer_group/instances/consumer_instance", url);
    }

    @Test
    public void testBuildSubscriptionsUrl() {
        String url = restProxyClient.buildSubscriptionsUrl("consumer_group2", "consumer_instance2");
        Assert.assertEquals("http://localhost:8080/consumers/consumer_group2/instances/consumer_instance2/subscription", url);
    }

    @Test
    public void testBuildConsumerInstanceRecordsUrl() {
        String url = restProxyClient.buildConsumerInstanceRecordsUrl("cg", "ci");
        Assert.assertEquals("http://localhost:8080/consumers/cg/instances/ci/records", url);
    }

    @Test
    public void testBuildTopicUrl() {
        String url = restProxyClient.buildTopicUrl("MyTopic");
        Assert.assertEquals("http://localhost:8080/topics/MyTopic", url);
    }
}
