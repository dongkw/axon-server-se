package io.axoniq.axonserver.metric;

import io.axoniq.axonserver.grpc.internal.Metric;

import java.util.function.Function;

/**
 * @author Marc Gathier
 */
public interface MetricCollector extends Function<String, ClusterMetric>  {

    Iterable<Metric> getAll();
}
