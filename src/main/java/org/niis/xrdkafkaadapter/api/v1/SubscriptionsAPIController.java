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
package org.niis.xrdkafkaadapter.api.v1;

import org.niis.xrd4j.rest.ClientResponse;
import org.niis.xrdkafkaadapter.kafka.client.RESTProxyClient;
import org.niis.xrdkafkaadapter.service.HelperService;
import org.niis.xrdkafkaadapter.util.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * This class implements a REST API for accessing record related functions.
 *
 */
@RestController
public class SubscriptionsAPIController {

    private static final Logger LOG = LoggerFactory.getLogger(SubscriptionsAPIController.class);

    @Autowired
    private HelperService helperService;

    @Autowired
    private RESTProxyClient proxyClient;

    /**
     * Subscribe to a Kafka topic.
     * @return
     */
    @RequestMapping(method = POST, path = Constants.API_BASE_PATH + "/{topicName}/subscriptions",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> subscribe(@RequestHeader(Constants.XRD_CLIENT_ID) String xrdClientId,
                                            @PathVariable String topicName) {
        LOG.info("Subscribe to topic \"{}\"", topicName);
        LOG.debug("X-Road-Client: \"{}\"", xrdClientId);

        ClientResponse response = proxyClient.subscribe(xrdClientId, topicName);
        return ResponseEntity.status(response.getStatusCode()).body(response.getData());
    }

    /**
     * Unsubscribe from a Kafka topic.
     * @return
     */
    @RequestMapping(method = DELETE, path = Constants.API_BASE_PATH + "/{topicName}/subscriptions",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> unsubscribe(@RequestHeader(Constants.XRD_CLIENT_ID) String xrdClientId,
                                              @PathVariable String topicName) {
        LOG.info("Unsubscribe from topic \"{}\"", topicName);
        LOG.debug("X-Road-Client: \"{}\"", xrdClientId);
        ClientResponse response = proxyClient.unsubscribe(xrdClientId, topicName);
        return ResponseEntity.status(response.getStatusCode()).body(response.getData());
    }
}
