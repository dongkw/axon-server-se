package io.axoniq.axonserver.rest;

import io.axoniq.axonserver.KeepNames;
import io.axoniq.axonserver.enterprise.cluster.ClusterController;
import io.axoniq.axonserver.enterprise.jpa.ClusterNode;
import io.axoniq.axonserver.exception.ErrorCode;
import io.axoniq.axonserver.exception.MessagingPlatformException;
import io.axoniq.axonserver.enterprise.cluster.internal.ClusterJoinRequester;
import io.axoniq.axonserver.features.Feature;
import io.axoniq.axonserver.features.FeatureChecker;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Author: marc
 */
@RestController("ClusterRestController")
@RequestMapping("/v1/cluster")
public class ClusterRestController {

    private final ClusterController clusterController;
    private final ClusterJoinRequester clusterJoinRequester;
    private final FeatureChecker limits;


    public ClusterRestController(ClusterController clusterController, ClusterJoinRequester clusterJoinRequester,
                                 FeatureChecker limits) {
        this.clusterController = clusterController;
        this.clusterJoinRequester = clusterJoinRequester;
        this.limits = limits;
    }


    @PostMapping
    public Future<Void> add(@Valid @RequestBody ClusterJoinRequest jsonClusterNode) {
        if( !Feature.CLUSTERING.enabled(limits) ) {
            throw new MessagingPlatformException(ErrorCode.CLUSTER_NOT_ALLOWED, "License does not allow clustering of AxonHub servers");
        }
        if( clusterController.getRemoteConnections().size() >= limits.getMaxClusterSize()) {
            throw new MessagingPlatformException(ErrorCode.MAX_CLUSTER_SIZE_REACHED, "Maximum allowed number of nodes reached");
        }

        if( jsonClusterNode.contexts != null && ! jsonClusterNode.contexts.isEmpty()) {
            clusterController.setMyContexts(jsonClusterNode.contexts);
        }
        return clusterJoinRequester.addNode(jsonClusterNode.internalHostName, jsonClusterNode.internalGrpcPort);
    }

    @DeleteMapping( path = "{name}")
    public void deleteNode(@PathVariable("name") String name) {
        clusterController.sendDeleteNode(name);
    }


    @GetMapping
    public List<JsonClusterNode> list() {
        Stream<JsonClusterNode> otherNodes = clusterController.getRemoteConnections().stream().map(e -> JsonClusterNode.from(e.getClusterNode(), e.isConnected()));
        return Stream.concat(Stream.of(JsonClusterNode.from(clusterController.getMe(), true)), otherNodes).collect(Collectors.toList());
    }

    @GetMapping(path="{name}")
    public JsonClusterNode getOne(@PathVariable("name") String name) {
        ClusterNode node = clusterController.getNode(name);
        if( node == null ) throw new MessagingPlatformException(ErrorCode.NO_SUCH_APPLICATION, "Node " + name + " not found");

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
    private static class ClusterJoinRequest {
        @NotNull(message = "missing required field: internalHostName")
        private String internalHostName;
        @NotNull(message = "missing required field: internalGrpcPort")
        private Integer internalGrpcPort;

        private List<ContextRoleJSON> contexts;

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

        public List<ContextRoleJSON> getContexts() {
            return contexts;
        }

        public void setContexts(List<ContextRoleJSON> contexts) {
            this.contexts = contexts;
        }
    }

    public static class ContextRoleJSON {
        private String name;
        private boolean storage;
        private boolean messaging;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isStorage() {
            return storage;
        }

        public void setStorage(boolean storage) {
            this.storage = storage;
        }

        public boolean isMessaging() {
            return messaging;
        }

        public void setMessaging(boolean messaging) {
            this.messaging = messaging;
        }
    }
}