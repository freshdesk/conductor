/*
 * Copyright 2022 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.tasks.apiaction.providers;

import com.netflix.conductor.tasks.apiaction.ApiActionTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Optional;

/**
 * Provider for a customized RestTemplateBuilder. This class provides a default {@link
 * RestTemplateBuilder} which can be configured or extended as needed.
 */
@Component
public class DefaultRestApiTemplateProvider implements RestApiTemplateProvider {

    private final ThreadLocal<RestTemplate> threadLocalRestTemplate;

    private final int defaultReadTimeout;
    private final int defaultConnectTimeout;
    // Add this file
    @Autowired
    public DefaultRestApiTemplateProvider(
            @Value("${conductor.tasks.apiAction.readTimeout:150ms}") Duration readTimeout,
            @Value("${conductor.tasks.apiAction.connectTimeout:100ms}") Duration connectTimeout) {
        this.threadLocalRestTemplate = ThreadLocal.withInitial(RestTemplate::new);
        this.defaultReadTimeout = (int) readTimeout.toMillis();
        this.defaultConnectTimeout = (int) connectTimeout.toMillis();
    }

    @Override
    public @NonNull RestTemplate getRestTemplate(@NonNull ApiActionTask.Input input) {
        RestTemplate restTemplate = threadLocalRestTemplate.get();
        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectTimeout(
                Optional.ofNullable(input.getConnectionTimeOut()).orElse(defaultConnectTimeout));
        requestFactory.setReadTimeout(
                Optional.ofNullable(input.getReadTimeOut()).orElse(defaultReadTimeout));
        restTemplate.setRequestFactory(requestFactory);
        return restTemplate;
    }
}
