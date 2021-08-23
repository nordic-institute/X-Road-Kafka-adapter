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
package org.niis.xrdkafkaadapter.util;

/**
 * This class contains constants used in other classes.
 */
public final class Constants {

    private Constants() { }

    public static final String API_BASE_PATH = "/api/v1";

    public static final String XRD_CLIENT_ID = "X-Road-Client";

    public static final String XRD_SERVICE_ID = "X-Road-Service";

    public static final String SERVICE_ID_TOPIC_MAPPING_PROPERTY_KEY = "app.service-id-topic-mapping.";

    public static final String KAFKA_BROKER_URL_PROPERTY_KEY = "app.kafka.broker-url";

    public static final String KAFKA_CONSUMER_GROUP_POSTFIX = "_group";

    public static final String KAFKA_CONSUMER_INSTANCE_POSTFIX = "_instance";

    public static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";

    public static final String HTTP_HEADER_ACCEPT = "Accept";

    public static final String CONTENT_TYPE_KAFKA_JSON_V2 = "application/vnd.kafka.json.v2+json";
}
