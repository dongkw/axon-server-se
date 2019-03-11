package io.axoniq.axonserver.enterprise.cluster;

import io.axoniq.axonserver.enterprise.cluster.events.ClusterEvents;
import io.axoniq.axonserver.enterprise.context.ContextController;
import io.axoniq.axonserver.grpc.internal.Context;
import io.axoniq.axonserver.grpc.internal.ContextMember;
import io.axoniq.axonserver.grpc.internal.State;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * @author Marc Gathier
 */
@Controller
public class GrpcRaftStatusManager {
    private final ContextController contextController;
    private final RaftGroupServiceFactory raftServiceFactory;
    private final RaftLeaderProvider raftLeaderProvider;
    private final ApplicationEventPublisher eventPublisher;

    public GrpcRaftStatusManager(ContextController contextController,
                                 RaftGroupServiceFactory raftServiceFactory,
                                 RaftLeaderProvider raftLeaderProvider,
                                 ApplicationEventPublisher eventPublisher) {
        this.contextController = contextController;
        this.raftServiceFactory = raftServiceFactory;
        this.raftLeaderProvider = raftLeaderProvider;
        this.eventPublisher = eventPublisher;
    }

    /**
     * At a fixed delay, request a status update from all nodes. Status includes what nodes are leaders within which context.
     * Sent to all nodes, each node will reply for its contexts
     */
    @Scheduled(fixedDelay = 5000)
    public void updateStatus() {
        contextController.getRemoteNodes().forEach(node -> raftServiceFactory.getRaftGroupServiceForNode(node)
                                                                       .getStatus(this::updateLeader));
    }

    private void updateLeader(Context context) {
        List<ContextMember> membersList = context.getMembersList();
        ContextMember leader = membersList.stream()
                                          .filter(cm -> State.LEADER.getNumber() == cm.getState().getNumber())
                                          .findFirst()
                                          .orElse(null);
        if (leader != null && !leader.getNodeName().equals(raftLeaderProvider.getLeader(context.getName()))){
            eventPublisher.publishEvent(new ClusterEvents.LeaderConfirmation(context.getName(),
                                                                             leader.getNodeName(),
                                                                             true));
        }

        if (leader == null && raftLeaderProvider.getLeader(context.getName()) != null) {
            eventPublisher.publishEvent(new ClusterEvents.LeaderConfirmation(context.getName(),
                                                                             null,
                                                                             true));
        }
    }
}
