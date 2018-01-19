/**
 * Copyright 2017 Pivotal Software, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.spring.autoconfigure.export.prometheus;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.lang.NonNullApi;
import io.micrometer.core.lang.Nullable;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.micrometer.spring.autoconfigure.export.MetricsExporter;
import io.micrometer.spring.autoconfigure.export.StringToDurationConverter;
import io.prometheus.client.CollectorRegistry;
import org.springframework.boot.actuate.autoconfigure.ManagementContextConfiguration;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Duration;

/**
 * Configuration for exporting metrics to Prometheus.
 *
 * @author Jon Schneider
 */
@Configuration
@ConditionalOnClass(PrometheusMeterRegistry.class)
@EnableConfigurationProperties(PrometheusProperties.class)
@Import(StringToDurationConverter.class)
public class PrometheusExportConfiguration {

    @NonNullApi
    private class DefaultPrometheusConfig implements PrometheusConfig {
        private final PrometheusProperties props;
        private final PrometheusConfig defaults = k -> null;

        private DefaultPrometheusConfig(PrometheusProperties props) {
            this.props = props;
        }

        @Override
        @Nullable
        public String get(String k) {
            return null;
        }

        @Override
        public boolean descriptions() {
            return props.getDescriptions() == null ? defaults.descriptions() : props.getDescriptions();
        }

        @Override
        public Duration step() {
            return props.getStep() == null ? defaults.step() : props.getStep();
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public PrometheusConfig prometheusConfig(PrometheusProperties props) {
        return new DefaultPrometheusConfig(props);
    }

    @Bean
    @ConditionalOnProperty(value = "management.metrics.export.prometheus.enabled", matchIfMissing = true)
    public MetricsExporter prometheusExporter(PrometheusConfig config,
                                              CollectorRegistry collectorRegistry, Clock clock) {
        return () -> new PrometheusMeterRegistry(config, collectorRegistry, clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public CollectorRegistry collectorRegistry() {
        return new CollectorRegistry(true);
    }

    @Bean
    @ConditionalOnMissingBean
    public Clock micrometerClock() {
        return Clock.SYSTEM;
    }

    @ManagementContextConfiguration
    @ConditionalOnClass(AbstractEndpoint.class)
    public static class PrometheusScrapeEndpointConfiguration {
        @Bean
        public PrometheusScrapeEndpoint prometheusEndpoint(
            CollectorRegistry collectorRegistry) {
            return new PrometheusScrapeEndpoint(collectorRegistry);
        }
    }
}
