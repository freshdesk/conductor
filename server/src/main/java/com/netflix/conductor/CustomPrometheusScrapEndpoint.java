package com.netflix.conductor;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;

@Endpoint(id="custommetrics")
@Component
public class CustomPrometheusScrapEndpoint {
    @ReadOperation
    @Bean
    public String scrapCustomMetrics() {
        PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT,
                                                                            CollectorRegistry.defaultRegistry,
                                                                            Clock.SYSTEM);
        return prometheusRegistry.scrape();
    }
}
