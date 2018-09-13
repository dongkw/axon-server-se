package io.axoniq.axonserver.enterprise.cluster.internal;

import io.axoniq.axonserver.enterprise.jpa.ClusterNode;
import io.axoniq.axonserver.config.MessagingPlatformConfiguration;
import io.axoniq.axonserver.enterprise.cluster.internal.MessagingClusterServiceInterface;
import io.axoniq.axonserver.grpc.DataSychronizationServiceInterface;

/**
 * Author: marc
 */
public interface StubFactory {
    MessagingClusterServiceInterface messagingClusterServiceStub(MessagingPlatformConfiguration messagingPlatformConfiguration, ClusterNode clusterNode);

    MessagingClusterServiceInterface messagingClusterServiceStub(MessagingPlatformConfiguration messagingPlatformConfiguration, String host, int port);

    DataSychronizationServiceInterface dataSynchronizationServiceStub(MessagingPlatformConfiguration messagingPlatformConfiguration, ClusterNode clusterNode);

}