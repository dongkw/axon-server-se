package io.axoniq.axonserver.enterprise.cluster;

import java.util.Collection;
import java.util.List;

/**
 * @author Marc Gathier
 */
public interface NodeSelectionStrategy {
    String selectNode(String clientName, String componentName, Collection<String> activeNodes);

    boolean canRebalance(String clientName, String componentName, List<String> activeNodes);
}
