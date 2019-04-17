package io.axoniq.axonserver.rest;

import io.axoniq.axonserver.KeepNames;
import io.axoniq.axonserver.enterprise.cluster.ClusterController;
import io.axoniq.axonserver.enterprise.cluster.GrpcRaftController;
import io.axoniq.axonserver.enterprise.cluster.RaftConfigServiceFactory;
import io.axoniq.axonserver.enterprise.context.ContextNameValidation;
import io.axoniq.axonserver.enterprise.jpa.ClusterNode;
import io.axoniq.axonserver.exception.ErrorCode;
import io.axoniq.axonserver.exception.MessagingPlatformException;
import io.axoniq.axonserver.features.Feature;
import io.axoniq.axonserver.features.FeatureChecker;
import io.axoniq.axonserver.grpc.internal.ContextRole;
import io.axoniq.axonserver.grpc.internal.NodeInfo;
import io.axoniq.axonserver.rest.json.RestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import static io.axoniq.axonserver.RaftAdminGroup.isAdmin;

/**
 * @author Marc Gathier
 */
@RestController("ClusterRestController")
@RequestMapping("/v1/cluster")
public class ClusterRestController {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ClusterController clusterController;
    private final RaftConfigServiceFactory raftServiceFactory;
    private final GrpcRaftController grpcRaftController;
    private final FeatureChecker limits;
    private final Predicate<String> contextNameValidation = new ContextNameValidation();

    public ClusterRestController(ClusterController clusterController,
                                 RaftConfigServiceFactory raftServiceFactory,
                                 GrpcRaftController grpcRaftController,
                                 FeatureChecker limits) {
        this.clusterController = clusterController;
        this.raftServiceFactory = raftServiceFactory;
        this.grpcRaftController = grpcRaftController;
        this.limits = limits;
    }


    @PostMapping
    public ResponseEntity<RestResponse> add(@Valid @RequestBody ClusterJoinRequest jsonClusterNode) {
        if (!Feature.CLUSTERING.enabled(limits)) {
            return new RestResponse(false, "License does not allow clustering of Axon servers")
                    .asResponseEntity(ErrorCode.CLUSTER_NOT_ALLOWED);
        }
        if (!grpcRaftController.getContexts().isEmpty()) {
            return new RestResponse(false, "Server already has contexts defined, cannot join cluster")
                    .asResponseEntity(ErrorCode.ALREADY_MEMBER_OF_CLUSTER);
        }

        NodeInfo.Builder nodeInfoBuilder = NodeInfo.newBuilder(clusterController.getMe().toNodeInfo());
        String context = jsonClusterNode.getContext();
        // Check for both context and noContext
        if (context != null && !context.isEmpty()) {
            if (!isAdmin(context) && !contextNameValidation.test(context)) {
                throw new MessagingPlatformException(ErrorCode.INVALID_CONTEXT_NAME,
                                                     "Invalid context name: " + context);
            }
            logger.debug("add(): Registering myself and adding me to context \"{}\".", context);
            nodeInfoBuilder.addContexts(ContextRole.newBuilder().setName(context).build());
        } else if ((jsonClusterNode.getNoContexts() != null) && jsonClusterNode.getNoContexts()) {
            logger.debug("add(): Registering myself and adding me to no contexts.");
            nodeInfoBuilder.addContexts(ContextRole.newBuilder().setName("_none").build());
        } else {
            logger.debug("add(): Registering myself and adding me to all contexts.");
        }

        try {
            raftServiceFactory.getRaftConfigServiceStub(jsonClusterNode.internalHostName,
                                                        jsonClusterNode.internalGrpcPort)
                              .joinCluster(nodeInfoBuilder.build());

            return ResponseEntity.accepted()
                                 .body(new RestResponse(true,
                                                        "Accepted join request, may take a while to process"));
        } catch (Exception ex) {
            return new RestResponse(false, ex.getMessage()).asResponseEntity(ErrorCode.fromException(ex));
        }
    }


    @DeleteMapping( path = "{name}")
    public void deleteNode(@PathVariable("name") String name) {
        if( !Feature.CLUSTERING.enabled(limits) ) {
            throw new MessagingPlatformException(ErrorCode.CLUSTER_NOT_ALLOWED, "License does not allow clustering of Axon servers");
        }
        raftServiceFactory.getRaftConfigService().deleteNode(name);
    }


    @GetMapping
    public List<JsonClusterNode> list() {
        Stream<JsonClusterNode> otherNodes = clusterController.getRemoteConnections().stream().map(e -> JsonClusterNode.from(e.getClusterNode(), e.isConnected()));
        return Stream.concat(Stream.of(JsonClusterNode.from(clusterController.getMe(), true)), otherNodes).collect(Collectors.toList());
    }

    @GetMapping(path="{name}")
    public JsonClusterNode getOne(@PathVariable("name") String name) {
        ClusterNode node = clusterController.getNode(name);
        if( node == null ) throw new MessagingPlatformException(ErrorCode.NO_SUCH_NODE, "Node " + name + " not found");

        return JsonClusterNode.from(node, true);
    }

    @KeepNames
    public static class JsonClusterNode {
        private String name;
        private String internalHostName;
        private Integer internalGrpcPort;
        private String hostName;
        private Integer grpcPort;
        private Integer httpPort;
        private boolean connected;

        public boolean isConnected() {
            return connected;
        }

        public void setConnected(boolean connected) {
            this.connected = connected;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getInternalHostName() {
            return internalHostName;
        }

        public void setInternalHostName(String internalHostName) {
            this.internalHostName = internalHostName;
        }

        public String getHostName() {
            return hostName;
        }

        public void setHostName(String hostName) {
            this.hostName = hostName;
        }

        public Integer getHttpPort() {
            return httpPort;
        }

        public void setHttpPort(Integer httpPort) {
            this.httpPort = httpPort;
        }

        public Integer getInternalGrpcPort() {
            return internalGrpcPort;
        }

        public void setInternalGrpcPort(Integer internalGrpcPort) {
            this.internalGrpcPort = internalGrpcPort;
        }

        public Integer getGrpcPort() {
            return grpcPort;
        }

        public void setGrpcPort(Integer grpcPort) {
            this.grpcPort = grpcPort;
        }

        public static JsonClusterNode from(ClusterNode jpaClusterNode, boolean connected) {
            JsonClusterNode clusterNode = new JsonClusterNode();
            clusterNode.name = jpaClusterNode.getName();
            clusterNode.internalHostName = jpaClusterNode.getInternalHostName();
            clusterNode.internalGrpcPort = jpaClusterNode.getGrpcInternalPort();
            clusterNode.hostName = jpaClusterNode.getHostName();
            clusterNode.grpcPort = jpaClusterNode.getGrpcPort();
            clusterNode.httpPort = jpaClusterNode.getHttpPort();
            clusterNode.connected = connected;
            return clusterNode;
        }
    }

    @KeepNames
    public static class ClusterJoinRequest {
        @NotNull(message = "missing required field: internalHostName")
        private String internalHostName;
        @NotNull(message = "missing required field: internalGrpcPort")
        private Integer internalGrpcPort;

        private String context;
        private Boolean noContexts;

        public String getInternalHostName() {
            return internalHostName;
        }

        public void setInternalHostName(String internalHostName) {
            this.internalHostName = internalHostName;
        }

        public Integer getInternalGrpcPort() {
            return internalGrpcPort;
        }

        public void setInternalGrpcPort(@NotNull Integer internalGrpcPort) {
            this.internalGrpcPort = internalGrpcPort;
        }

        public String getContext() {
            return context;
        }

        public void setContext(String context) {
            this.context = context;
        }

        public Boolean getNoContexts() {
            return noContexts;
        }

        public void setNoContexts(Boolean noContexts) {
            this.noContexts = noContexts;
        }
    }
}
