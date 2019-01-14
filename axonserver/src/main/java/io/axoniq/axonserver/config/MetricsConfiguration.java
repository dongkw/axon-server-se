package io.axoniq.axonserver.config;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @author Marc Gathier
 */
@Configuration
public class MetricsConfiguration {

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(MessagingPlatformConfiguration messagingPlatformConfiguration) {
        MeterFilter defaultFilter = new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, @NotNull DistributionStatisticConfig config) {
                if (id.getName().startsWith("axon")) {
                    return DistributionStatisticConfig.builder()
                                                      .percentiles(0.5, 0.95, 0.99)
                                                      .minimumExpectedValue(TimeUnit.MILLISECONDS.toMillis(1))
                                                      .maximumExpectedValue(TimeUnit.SECONDS.toMillis(10))
                                                      .expiry(Duration.ofMinutes(messagingPlatformConfiguration.getMetricsInterval()))
                                                      .build()
                                                      .merge(config);
                }
                return config;
            }
        };
        return registry -> registry.config().meterFilter(defaultFilter).
                commonTags("axonserver", messagingPlatformConfiguration.getName());
    }
}
